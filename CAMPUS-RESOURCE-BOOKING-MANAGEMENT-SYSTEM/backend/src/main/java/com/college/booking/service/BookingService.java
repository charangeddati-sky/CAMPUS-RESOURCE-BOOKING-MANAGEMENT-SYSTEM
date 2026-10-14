package com.college.booking.service;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.dto.BookingDtos.CalendarEvent;
import com.college.booking.dto.BookingDtos.CreateBookingRequest;
import com.college.booking.dto.BookingDtos.DecisionRequest;
import com.college.booking.entity.Booking;
import com.college.booking.entity.BookingApproval;
import com.college.booking.entity.BookingEquipment;
import com.college.booking.entity.BookingResource;
import com.college.booking.entity.Equipment;
import com.college.booking.entity.Resource;
import com.college.booking.entity.User;
import com.college.booking.enums.ApprovalStatus;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.NotificationType;
import com.college.booking.enums.RecurrenceType;
import com.college.booking.enums.Role;
import com.college.booking.exception.ApiException;
import com.college.booking.mapper.DtoMapper;
import com.college.booking.repository.BookingApprovalRepository;
import com.college.booking.repository.BookingEquipmentRepository;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.EquipmentRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final BookingApprovalRepository approvalRepository;
    private final BookingEquipmentRepository bookingEquipmentRepository;
    private final ResourceRepository resourceRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final WaitlistService waitlistService;
    private final OccupancyService occupancyService;
    private final DtoMapper mapper;

    public BookingService(BookingRepository bookingRepository,
                          BookingResourceRepository bookingResourceRepository,
                          BookingApprovalRepository approvalRepository,
                          BookingEquipmentRepository bookingEquipmentRepository,
                          ResourceRepository resourceRepository,
                          EquipmentRepository equipmentRepository,
                          UserRepository userRepository,
                          AvailabilityService availabilityService,
                          NotificationService notificationService,
                          AuditService auditService,
                          WaitlistService waitlistService,
                          OccupancyService occupancyService,
                          DtoMapper mapper) {
        this.bookingRepository = bookingRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.approvalRepository = approvalRepository;
        this.bookingEquipmentRepository = bookingEquipmentRepository;
        this.resourceRepository = resourceRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.waitlistService = waitlistService;
        this.occupancyService = occupancyService;
        this.mapper = mapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingView create(User actor, CreateBookingRequest req) {
        if (req.resourceIds() == null || req.resourceIds().isEmpty()) {
            throw ApiException.badRequest("NO_RESOURCES", "Select at least one resource.");
        }
        RecurrenceType recurrence = req.recurrenceType() == null ? RecurrenceType.NONE : req.recurrenceType();
        List<LocalDate> dates = expandDates(req.date(), recurrence, req.recurrenceEndDate());
        Booking first = null;
        for (LocalDate date : dates) {
            Booking created = createSingle(actor, req, date, first);
            if (first == null) {
                first = created;
            }
        }
        occupancyService.invalidateAfterCommit();
        return toView(first);
    }

    private Booking createSingle(User actor, CreateBookingRequest req, LocalDate date, Booking parent) {
        User lockedUser = userRepository.lockById(actor.getId())
                .orElseThrow(() -> ApiException.notFound("User not found."));
        List<Booking> doubles = bookingRepository.findUserDoubleBookings(
                lockedUser.getId(), date, req.startTime(), req.endTime(), AvailabilityService.ACTIVE);
        if (!doubles.isEmpty()) {
            throw ApiException.conflict("USER_DOUBLE_BOOKING",
                    "You already have a booking that overlaps " + date + " " + req.startTime() + "–" + req.endTime() + ".");
        }

        List<Long> resourceIds = new ArrayList<>(req.resourceIds());
        resourceIds.sort(Comparator.naturalOrder());
        List<Resource> locked = new ArrayList<>();
        for (Long id : resourceIds) {
            Resource resource = resourceRepository.lockById(id)
                    .orElseThrow(() -> ApiException.notFound("Resource " + id + " does not exist."));
            String reason = availabilityService.unavailableReason(resource, date, req.startTime(), req.endTime());
            if (reason != null) {
                throw ApiException.conflict("BOOKING_CONFLICT", reason);
            }
            locked.add(resource);
        }

        if (req.equipment() != null) {
            List<com.college.booking.dto.BookingDtos.EquipmentQty> equipment = new ArrayList<>(req.equipment());
            equipment.sort(Comparator.comparing(com.college.booking.dto.BookingDtos.EquipmentQty::equipmentId));
            for (var eq : equipment) {
                Equipment item = equipmentRepository.lockById(eq.equipmentId())
                        .orElseThrow(() -> ApiException.notFound("Equipment not found."));
                int qty = eq.quantity() == null ? 1 : eq.quantity();
                if (item.getAvailable() < qty) {
                    throw ApiException.conflict("EQUIPMENT_UNAVAILABLE",
                            item.getName() + " does not have " + qty + " units available.");
                }
            }
        }

        Booking booking = new Booking();
        booking.setUser(lockedUser);
        booking.setTitle(req.title() == null || req.title().isBlank()
                ? locked.get(0).getName() + " booking" : req.title());
        booking.setPurpose(req.purpose());
        booking.setBookingDate(date);
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setAttendees(req.attendees() == null ? 0 : req.attendees());
        booking.setRequirements(req.requirements());
        booking.setRecurrenceType(req.recurrenceType() == null ? RecurrenceType.NONE : req.recurrenceType());
        booking.setRecurrenceEndDate(req.recurrenceEndDate());
        booking.setParentBooking(parent);
        booking.setBookingKind(req.bookingKind() == null ? "RESOURCE" : req.bookingKind());
        booking.setCheckInToken(UUID.randomUUID().toString());
        booking.setStatus(initialStatus(lockedUser.getRole()));
        bookingRepository.saveAndFlush(booking);

        for (Resource resource : locked) {
            BookingResource br = new BookingResource();
            br.setBooking(booking);
            br.setResource(resource);
            bookingResourceRepository.save(br);
        }
        bookingResourceRepository.flush();

        for (Resource resource : locked) {
            List<Booking> conflicts = bookingRepository.findConflicts(
                    resource.getId(), date, req.startTime(), req.endTime(), AvailabilityService.ACTIVE);
            boolean collision = conflicts.stream().anyMatch(c -> !c.getId().equals(booking.getId()));
            if (collision) {
                throw ApiException.conflict("BOOKING_CONFLICT",
                        resource.getName() + " was booked by someone else at the same moment.");
            }
        }

        if (req.equipment() != null) {
            List<com.college.booking.dto.BookingDtos.EquipmentQty> equipment = new ArrayList<>(req.equipment());
            equipment.sort(Comparator.comparing(com.college.booking.dto.BookingDtos.EquipmentQty::equipmentId));
            for (var eq : equipment) {
                Equipment item = equipmentRepository.lockById(eq.equipmentId()).orElseThrow();
                int qty = eq.quantity() == null ? 1 : eq.quantity();
                if (item.getAvailable() < qty) {
                    throw ApiException.conflict("EQUIPMENT_UNAVAILABLE",
                            item.getName() + " does not have " + qty + " units available.");
                }
                item.setAvailable(item.getAvailable() - qty);
                BookingEquipment be = new BookingEquipment();
                be.setBooking(booking);
                be.setEquipment(item);
                be.setQuantity(qty);
                bookingEquipmentRepository.save(be);
            }
        }

        createApprovals(booking, lockedUser);
        notifyOnCreate(booking, lockedUser, locked);
        auditService.record(lockedUser, "CREATE_BOOKING", "Booking", booking.getId(),
                booking.getTitle() + " on " + date);
        return booking;
    }

    private BookingStatus initialStatus(Role role) {
        return switch (role) {
            case ADMIN -> BookingStatus.CONFIRMED;
            case PROFESSOR -> BookingStatus.PENDING_ADMIN;
            case STUDENT -> BookingStatus.PENDING_PROFESSOR;
        };
    }

    private void createApprovals(Booking booking, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.STUDENT) {
            BookingApproval professor = new BookingApproval();
            professor.setBooking(booking);
            professor.setRequiredRole(Role.PROFESSOR);
            professor.setStatus(ApprovalStatus.PENDING);
            approvalRepository.save(professor);
        }
        BookingApproval admin = new BookingApproval();
        admin.setBooking(booking);
        admin.setRequiredRole(Role.ADMIN);
        admin.setStatus(actor.getRole() == Role.PROFESSOR ? ApprovalStatus.PENDING : ApprovalStatus.PENDING);
        approvalRepository.save(admin);
    }

    private void notifyOnCreate(Booking booking, User actor, List<Resource> resources) {
        String names = resources.stream().map(Resource::getName).reduce((a, b) -> a + ", " + b).orElse("resource");
        notificationService.notify(actor, NotificationType.BOOKING_SUBMITTED, "Booking submitted",
                "Your request for " + names + " on " + booking.getBookingDate() + " is " + pretty(booking.getStatus()) + ".",
                "/bookings/" + booking.getId());
        if (booking.getStatus() == BookingStatus.PENDING_PROFESSOR) {
            User professor = actor.getAssignedProfessor();
            if (professor != null) {
                notificationService.notify(professor, NotificationType.PROFESSOR_APPROVAL, "Approval needed",
                        actor.getFullName() + " requested " + names + " on " + booking.getBookingDate() + ".",
                        "/approvals");
            } else {
                userRepository.findByRole(Role.PROFESSOR).forEach(p ->
                        notificationService.notify(p, NotificationType.PROFESSOR_APPROVAL, "Approval needed",
                                actor.getFullName() + " requested " + names + ".", "/approvals"));
            }
        } else if (booking.getStatus() == BookingStatus.PENDING_ADMIN) {
            userRepository.findByRole(Role.ADMIN).forEach(a ->
                    notificationService.notify(a, NotificationType.ADMIN_APPROVAL, "Admin approval needed",
                            actor.getFullName() + " requested " + names + ".", "/approvals"));
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            notificationService.notify(actor, NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
                    names + " is confirmed for " + booking.getBookingDate() + ".", "/bookings/" + booking.getId());
        }
    }

    @Transactional
    public BookingView approve(Long bookingId, User actor, DecisionRequest req) {
        Booking booking = load(bookingId);
        assertCanDecide(actor, booking);
        BookingApproval step = currentStep(booking, actor.getRole());
        step.setStatus(ApprovalStatus.APPROVED);
        step.setApprover(actor);
        step.setComment(req == null ? null : req.comment());
        step.setDecidedAt(Instant.now());
        approvalRepository.save(step);

        if (actor.getRole() == Role.PROFESSOR) {
            booking.setStatus(BookingStatus.PENDING_ADMIN);
            userRepository.findByRole(Role.ADMIN).forEach(admin ->
                    notificationService.notify(admin, NotificationType.ADMIN_APPROVAL, "Professor approved",
                            booking.getTitle() + " is waiting for admin confirmation.", "/approvals"));
        } else {
            booking.setStatus(BookingStatus.CONFIRMED);
            notificationService.notify(booking.getUser(), NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
                    booking.getTitle() + " is confirmed for " + booking.getBookingDate() + ".",
                    "/bookings/" + booking.getId());
        }
        bookingRepository.save(booking);
        occupancyService.invalidateAfterCommit();
        auditService.record(actor, "APPROVE_BOOKING", "Booking", booking.getId(), actor.getRole().name());
        return toView(booking);
    }

    @Transactional
    public BookingView reject(Long bookingId, User actor, DecisionRequest req) {
        Booking booking = load(bookingId);
        assertCanDecide(actor, booking);
        BookingApproval step = currentStep(booking, actor.getRole());
        step.setStatus(ApprovalStatus.REJECTED);
        step.setApprover(actor);
        step.setComment(req == null ? null : req.comment());
        step.setDecidedAt(Instant.now());
        approvalRepository.save(step);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(req == null || req.comment() == null || req.comment().isBlank()
                ? "Rejected by " + actor.getRole().name().toLowerCase() : req.comment());
        bookingRepository.save(booking);
        releaseEquipment(booking);
        notificationService.notify(booking.getUser(), NotificationType.BOOKING_REJECTED, "Booking rejected",
                booking.getTitle() + " was rejected. " + booking.getRejectionReason(),
                "/bookings/" + booking.getId());
        occupancyService.invalidateAfterCommit();
        auditService.record(actor, "REJECT_BOOKING", "Booking", booking.getId(), booking.getRejectionReason());
        return toView(booking);
    }

    @Transactional
    public BookingView cancel(Long bookingId, User actor) {
        Booking booking = load(bookingId);
        if (!actor.getRole().equals(Role.ADMIN) && !booking.getUser().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only cancel your own bookings.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.badRequest("INVALID_STATUS", "This booking cannot be cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        releaseEquipment(booking);
        notificationService.notify(booking.getUser(), NotificationType.BOOKING_CANCELLED, "Booking cancelled",
                booking.getTitle() + " was cancelled.", "/bookings/" + booking.getId());
        waitlistService.notifyNext(booking);
        occupancyService.invalidateAfterCommit();
        auditService.record(actor, "CANCEL_BOOKING", "Booking", booking.getId(), null);
        return toView(booking);
    }

    public BookingView get(Long id, User actor) {
        Booking booking = load(id);
        if (!actor.getRole().equals(Role.ADMIN) && !actor.getRole().equals(Role.PROFESSOR)
                && !booking.getUser().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You cannot view this booking.");
        }
        return toView(booking);
    }

    public List<BookingView> mine(User actor) {
        return bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(actor.getId())
                .stream().map(this::toView).toList();
    }

    public List<CalendarEvent> calendarEvents(User actor, LocalDate from, LocalDate to) {
        List<Booking> source = (actor.getRole() == Role.ADMIN || actor.getRole() == Role.PROFESSOR)
                ? bookingRepository.findBetween(from, to)
                : bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(actor.getId()).stream()
                .filter(b -> !b.getBookingDate().isBefore(from) && !b.getBookingDate().isAfter(to))
                .toList();
        return source.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getStatus() != BookingStatus.REJECTED)
                .map(b -> {
                    BookingView view = toView(b);
                    String resourceName = view.resources().isEmpty() ? "Campus" : view.resources().get(0).name();
                    String buildingName = view.resources().isEmpty() ? "" : view.resources().get(0).buildingName();
                    return new CalendarEvent(
                            b.getId(),
                            resourceName + " · " + b.getTitle(),
                            iso(b.getBookingDate(), b.getStartTime()),
                            iso(b.getBookingDate(), b.getEndTime()),
                            b.getStatus().name(),
                            calendarColor(b.getStatus()),
                            "/bookings/" + b.getId(),
                            resourceName,
                            buildingName
                    );
                })
                .toList();
    }

    private String iso(LocalDate date, LocalTime time) {
        return "%sT%02d:%02d:00".formatted(date, time.getHour(), time.getMinute());
    }

    private String calendarColor(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "#146c4a";
            case CHECKED_IN -> "#0f3d2e";
            case PENDING_PROFESSOR, PENDING_ADMIN -> "#ea580c";
            case COMPLETED -> "#64748b";
            case NO_SHOW -> "#dc2626";
            default -> "#94a3b8";
        };
    }

    public List<BookingView> pendingFor(User actor) {
        Role role = actor.getRole();
        BookingStatus expected = role == Role.PROFESSOR ? BookingStatus.PENDING_PROFESSOR : BookingStatus.PENDING_ADMIN;
        return bookingRepository.findByStatus(expected).stream()
                .filter(b -> role != Role.PROFESSOR || matchesProfessor(actor, b))
                .map(this::toView)
                .toList();
    }

    public List<BookingView> all() {
        return bookingRepository.findAll().stream().map(this::toView).toList();
    }

    public String ics(Long bookingId, User actor) {
        BookingView view = get(bookingId, actor);
        String dt = view.date().toString().replace("-", "");
        String start = dt + "T" + view.startTime().toString().replace(":", "") + "00";
        String end = dt + "T" + view.endTime().toString().replace(":", "") + "00";
        String loc = view.resources().isEmpty() ? "Campus" : view.resources().get(0).buildingName() + " " + view.resources().get(0).name();
        return """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//CampusOS//Booking//EN
                BEGIN:VEVENT
                UID:booking-%d@campusos
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                DESCRIPTION:%s
                LOCATION:%s
                END:VEVENT
                END:VCALENDAR
                """.formatted(view.id(), start, end, safe(view.title()), safe(view.purpose()), safe(loc));
    }

    Booking load(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found."));
    }

    BookingView toView(Booking booking) {
        return mapper.toBooking(
                booking,
                bookingResourceRepository.findByBookingId(booking.getId()),
                approvalRepository.findByBookingId(booking.getId())
        );
    }

    private boolean matchesProfessor(User professor, Booking booking) {
        User student = booking.getUser();
        return student.getAssignedProfessor() == null
                || student.getAssignedProfessor().getId().equals(professor.getId());
    }

    private BookingApproval currentStep(Booking booking, Role role) {
        return approvalRepository.findByBookingIdAndRequiredRole(booking.getId(), role)
                .orElseThrow(() -> ApiException.badRequest("NO_APPROVAL_STEP", "No approval step for this role."));
    }

    private void assertCanDecide(User actor, Booking booking) {
        if (actor.getRole() == Role.PROFESSOR && booking.getStatus() != BookingStatus.PENDING_PROFESSOR) {
            throw ApiException.badRequest("INVALID_STATUS", "This booking is not waiting for professor approval.");
        }
        if (actor.getRole() == Role.ADMIN && booking.getStatus() != BookingStatus.PENDING_ADMIN
                && booking.getStatus() != BookingStatus.PENDING_PROFESSOR) {
            throw ApiException.badRequest("INVALID_STATUS", "This booking is not waiting for approval.");
        }
        if (actor.getRole() == Role.STUDENT) {
            throw ApiException.forbidden("Students cannot approve bookings.");
        }
    }

    private void releaseEquipment(Booking booking) {
        bookingEquipmentRepository.findByBookingId(booking.getId()).forEach(be -> {
            Equipment item = be.getEquipment();
            item.setAvailable(item.getAvailable() + be.getQuantity());
        });
    }

    private List<LocalDate> expandDates(LocalDate start, RecurrenceType type, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        if (type == null || type == RecurrenceType.NONE) {
            dates.add(start);
            return dates;
        }
        LocalDate last = end == null ? start.plusMonths(3) : end;
        LocalDate cursor = start;
        DayOfWeek dow = start.getDayOfWeek();
        int day = start.getDayOfMonth();
        while (!cursor.isAfter(last)) {
            dates.add(cursor);
            cursor = switch (type) {
                case DAILY -> cursor.plusDays(1);
                case WEEKLY, CUSTOM -> cursor.plusWeeks(1);
                case MONTHLY -> {
                    LocalDate next = cursor.plusMonths(1);
                    int d = Math.min(day, next.lengthOfMonth());
                    yield next.withDayOfMonth(d);
                }
                default -> last.plusDays(1);
            };
            if (type == RecurrenceType.WEEKLY && cursor.getDayOfWeek() != dow) {
                cursor = cursor.with(dow);
            }
            if (dates.size() > 60) {
                throw ApiException.badRequest("RECURRENCE_TOO_LONG", "Recurrence generated too many occurrences. Shorten the end date.");
            }
        }
        return dates;
    }

    private String pretty(BookingStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\n", " ");
    }
}
