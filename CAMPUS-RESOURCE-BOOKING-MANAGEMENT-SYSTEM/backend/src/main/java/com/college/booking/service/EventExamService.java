package com.college.booking.service;

import com.college.booking.dto.BookingDtos.CreateBookingRequest;
import com.college.booking.dto.BookingDtos.ExamRequest;
import com.college.booking.dto.BookingDtos.EventRequest;
import com.college.booking.entity.CampusEvent;
import com.college.booking.entity.Examination;
import com.college.booking.entity.Resource;
import com.college.booking.entity.User;
import com.college.booking.enums.ResourceKind;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.CampusEventRepository;
import com.college.booking.repository.ExaminationRepository;
import com.college.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventExamService {

    private final CampusEventRepository eventRepository;
    private final ExaminationRepository examinationRepository;
    private final ResourceRepository resourceRepository;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;

    public EventExamService(CampusEventRepository eventRepository, ExaminationRepository examinationRepository,
                            ResourceRepository resourceRepository, AvailabilityService availabilityService,
                            BookingService bookingService) {
        this.eventRepository = eventRepository;
        this.examinationRepository = examinationRepository;
        this.resourceRepository = resourceRepository;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    @Transactional
    public Map<String, Object> createEvent(User organizer, EventRequest req) {
        if (req.resourceIds() == null || req.resourceIds().isEmpty()) {
            throw ApiException.badRequest("NO_RESOURCES", "Select at least one room or hall for the event.");
        }
        var booking = bookingService.create(organizer, new CreateBookingRequest(
                req.resourceIds(), null, req.date(), req.startTime(), req.endTime(),
                req.name(), req.description(), req.expectedAttendees(), req.requiredEquipment(),
                null, null, "EVENT"
        ));
        CampusEvent event = new CampusEvent();
        event.setName(req.name());
        event.setOrganizer(organizer);
        event.setEventDate(req.date());
        event.setStartTime(req.startTime());
        event.setEndTime(req.endTime());
        event.setExpectedAttendees(req.expectedAttendees());
        event.setDescription(req.description());
        event.setRequiredEquipment(req.requiredEquipment());
        event.setBooking(bookingService.load(booking.id()));
        eventRepository.save(event);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("eventId", event.getId());
        res.put("booking", booking);
        return res;
    }

    public List<CampusEvent> events() {
        return eventRepository.findAllByOrderByEventDateDesc();
    }

    @Transactional
    public Map<String, Object> createExam(User admin, ExamRequest req) {
        List<Resource> rooms = resourceRepository.findAll().stream()
                .filter(Resource::isEnabled)
                .filter(r -> r.getResourceType().getKind() == ResourceKind.CLASSROOM
                        || r.getResourceType().getKind() == ResourceKind.EXAMINATION_HALL
                        || r.getResourceType().getKind() == ResourceKind.SEMINAR_HALL)
                .sorted(Comparator.comparing(r -> r.getBuilding().getCode() + r.getCode()))
                .toList();

        List<Resource> allocated = new ArrayList<>();
        int remaining = req.requiredCapacity();
        StringBuilder summary = new StringBuilder();
        String currentBuilding = null;
        for (Resource room : rooms) {
            if (remaining <= 0) break;
            String reason = availabilityService.unavailableReason(room, req.date(), req.startTime(), req.endTime());
            if (reason != null) continue;
            allocated.add(room);
            remaining -= room.getCapacity() == null ? 0 : room.getCapacity();
            if (currentBuilding == null || !currentBuilding.equals(room.getBuilding().getName())) {
                currentBuilding = room.getBuilding().getName();
                summary.append("\n").append(currentBuilding).append(": ");
            }
            summary.append(room.getCode()).append(" ");
        }
        if (remaining > 0) {
            throw ApiException.conflict("INSUFFICIENT_CAPACITY",
                    "Could not allocate " + req.requiredCapacity() + " seats. Short by " + remaining + " after conflict checks.");
        }
        var booking = bookingService.create(admin, new CreateBookingRequest(
                allocated.stream().map(Resource::getId).toList(),
                null, req.date(), req.startTime(), req.endTime(),
                req.name(), "Examination seating", req.requiredCapacity(), null,
                null, null, "EXAM"
        ));
        Examination exam = new Examination();
        exam.setName(req.name());
        exam.setExamDate(req.date());
        exam.setStartTime(req.startTime());
        exam.setEndTime(req.endTime());
        exam.setRequiredCapacity(req.requiredCapacity());
        exam.setAllocationSummary(summary.toString().trim());
        exam.setCreatedBy(admin);
        exam.setBooking(bookingService.load(booking.id()));
        examinationRepository.save(exam);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("examId", exam.getId());
        res.put("allocated", allocated.stream().map(r -> Map.of(
                "id", r.getId(), "name", r.getName(), "code", r.getCode(),
                "building", r.getBuilding().getName(), "capacity", r.getCapacity()
        )).toList());
        res.put("summary", exam.getAllocationSummary());
        res.put("booking", booking);
        return res;
    }

    public List<Examination> exams() {
        return examinationRepository.findAllByOrderByExamDateDesc();
    }
}
