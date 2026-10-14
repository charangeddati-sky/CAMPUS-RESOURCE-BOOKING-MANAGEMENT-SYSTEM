package com.college.booking.service;

import com.college.booking.dto.CampusDtos.AvailabilityResponse;
import com.college.booking.dto.CampusDtos.HourSlot;
import com.college.booking.entity.Booking;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceBlock;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.MaintenanceRepository;
import com.college.booking.repository.ResourceBlockRepository;
import com.college.booking.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AvailabilityService {

    public static final Set<BookingStatus> ACTIVE = EnumSet.of(
            BookingStatus.PENDING_PROFESSOR,
            BookingStatus.PENDING_ADMIN,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ResourceBlockRepository blockRepository;
    private final Clock clock;
    private final LocalTime campusOpen;
    private final LocalTime campusClose;

    public AvailabilityService(
            ResourceRepository resourceRepository,
            BookingRepository bookingRepository,
            MaintenanceRepository maintenanceRepository,
            ResourceBlockRepository blockRepository,
            Clock clock,
            @Value("${app.working-hours-start}") String open,
            @Value("${app.working-hours-end}") String close
    ) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.blockRepository = blockRepository;
        this.clock = clock;
        this.campusOpen = LocalTime.parse(open);
        this.campusClose = LocalTime.parse(close);
    }

    public AvailabilityResponse check(Long resourceId, LocalDate date, LocalTime start, LocalTime end) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        String reason = unavailableReason(resource, date, start, end);
        if (reason == null) {
            return new AvailabilityResponse(true, "AVAILABLE", null);
        }
        return new AvailabilityResponse(false, "UNAVAILABLE", reason);
    }

    public String unavailableReason(Resource resource, LocalDate date, LocalTime start, LocalTime end) {
        if (!resource.isEnabled() || resource.getOperationalStatus() == ResourceStatus.OUT_OF_SERVICE) {
            return "Resource is out of service.";
        }
        if (resource.getOperationalStatus() == ResourceStatus.BLOCKED) {
            return "Resource is blocked.";
        }
        if (!resource.getBuilding().isBookable()) {
            return "This area does not accept bookings.";
        }
        if (end == null || start == null || !end.isAfter(start)) {
            return "End time must be after start time.";
        }
        LocalTime open = resource.getWorkingHoursStart() != null ? resource.getWorkingHoursStart() : campusOpen;
        LocalTime close = resource.getWorkingHoursEnd() != null ? resource.getWorkingHoursEnd() : campusClose;
        if (start.isBefore(open) || end.isAfter(close)) {
            return "Requested time is outside working hours (" + open + "–" + close + ").";
        }
        if (!maintenanceRepository.findActiveOnDate(resource.getId(), date).isEmpty()) {
            return "Resource is under maintenance on this date.";
        }
        List<ResourceBlock> blocks = blockRepository.findActiveOnDate(resource.getId(), date);
        for (ResourceBlock block : blocks) {
            if (block.getStartTime() == null || block.getEndTime() == null) {
                return "Resource is blocked: " + block.getReason();
            }
            if (start.isBefore(block.getEndTime()) && end.isAfter(block.getStartTime())) {
                return "Resource is blocked: " + block.getReason();
            }
        }
        List<Booking> conflicts = bookingRepository.findConflicts(resource.getId(), date, start, end, ACTIVE);
        if (!conflicts.isEmpty()) {
            Booking c = conflicts.get(0);
            return resource.getName() + " is already booked from " + c.getStartTime() + " to " + c.getEndTime() + ".";
        }
        return null;
    }

    public ResourceStatus liveStatus(Resource resource) {
        if (!resource.isEnabled() || resource.getOperationalStatus() == ResourceStatus.OUT_OF_SERVICE) {
            return ResourceStatus.OUT_OF_SERVICE;
        }
        LocalDate today = LocalDate.now(clock);
        if (!maintenanceRepository.findActiveOnDate(resource.getId(), today).isEmpty()
                || resource.getOperationalStatus() == ResourceStatus.MAINTENANCE) {
            return ResourceStatus.MAINTENANCE;
        }
        if (resource.getOperationalStatus() == ResourceStatus.BLOCKED
                || !blockRepository.findActiveOnDate(resource.getId(), today).isEmpty()) {
            return ResourceStatus.BLOCKED;
        }
        LocalTime now = LocalTime.now(clock);
        List<Booking> current = bookingRepository.findConflicts(resource.getId(), today, now, now.plusMinutes(1), ACTIVE);
        if (!current.isEmpty()) {
            boolean pending = current.stream().anyMatch(b ->
                    b.getStatus() == BookingStatus.PENDING_PROFESSOR || b.getStatus() == BookingStatus.PENDING_ADMIN);
            return pending ? ResourceStatus.PENDING : ResourceStatus.BOOKED;
        }
        return ResourceStatus.AVAILABLE;
    }

    public boolean isAvailableNow(Resource resource) {
        return liveStatus(resource) == ResourceStatus.AVAILABLE;
    }

    public List<HourSlot> timeline(Resource resource, LocalDate date) {
        LocalTime open = resource.getWorkingHoursStart() != null ? resource.getWorkingHoursStart() : campusOpen;
        LocalTime close = resource.getWorkingHoursEnd() != null ? resource.getWorkingHoursEnd() : campusClose;
        List<HourSlot> slots = new ArrayList<>();
        LocalTime cursor = open;
        while (cursor.isBefore(close)) {
            LocalTime next = cursor.plusHours(1);
            if (next.isAfter(close)) {
                next = close;
            }
            String reason = unavailableReason(resource, date, cursor, next);
            String label = cursor.toString();
            slots.add(new HourSlot(cursor.toString(), label, reason == null, reason));
            cursor = next;
        }
        return slots;
    }

    public int countAvailableNow(List<Resource> resources) {
        int n = 0;
        for (Resource r : resources) {
            if (isAvailableNow(r)) {
                n++;
            }
        }
        return n;
    }
}
