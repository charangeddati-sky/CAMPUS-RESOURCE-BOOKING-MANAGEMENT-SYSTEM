package com.college.booking.service;

import com.college.booking.dto.BookingDtos.WaitlistRequest;
import com.college.booking.entity.Booking;
import com.college.booking.entity.Resource;
import com.college.booking.entity.User;
import com.college.booking.entity.Waitlist;
import com.college.booking.enums.NotificationType;
import com.college.booking.enums.WaitlistStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final ResourceRepository resourceRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final NotificationService notificationService;
    private final int holdMinutes;

    public WaitlistService(WaitlistRepository waitlistRepository,
            ResourceRepository resourceRepository,
            BookingResourceRepository bookingResourceRepository,
            NotificationService notificationService,
            @Value("${app.waitlist-hold-minutes}") int holdMinutes) {
        this.waitlistRepository = waitlistRepository;
        this.resourceRepository = resourceRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.notificationService = notificationService;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    public Waitlist join(User user, WaitlistRequest req) {
        Resource resource = resourceRepository.findById(req.resourceId())
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        boolean exists = waitlistRepository.existsByResourceIdAndUserIdAndBookingDateAndStartTimeAndEndTimeAndStatus(
                resource.getId(), user.getId(), req.date(), req.startTime(), req.endTime(), WaitlistStatus.WAITING);
        if (exists) {
            throw ApiException.conflict("ALREADY_WAITLISTED", "You are already on the waitlist for this slot.");
        }
        Waitlist w = new Waitlist();
        w.setResource(resource);
        w.setUser(user);
        w.setBookingDate(req.date());
        w.setStartTime(req.startTime());
        w.setEndTime(req.endTime());
        w.setAttendees(req.attendees());
        w.setPurpose(req.purpose());
        w.setStatus(WaitlistStatus.WAITING);
        waitlistRepository.save(w);
        notificationService.notify(user, NotificationType.WAITLIST_AVAILABLE, "Joined waitlist",
                "You are on the waitlist for " + resource.getName() + " on " + req.date() + ".",
                "/resources/" + resource.getId());
        return w;
    }

    public List<Waitlist> mine(Long userId) {
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void notifyNext(Booking cancelled) {
        bookingResourceRepository.findByBookingId(cancelled.getId()).forEach(br ->
                offerNext(br.getResource(), cancelled.getBookingDate(), cancelled.getStartTime(), cancelled.getEndTime()));
    }

    @Transactional
    public void expireHolds() {
        List<Waitlist> expired = waitlistRepository.findByStatusAndExpiresAtBefore(WaitlistStatus.NOTIFIED, Instant.now());
        for (Waitlist w : expired) {
            w.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(w);
            offerNext(w.getResource(), w.getBookingDate(), w.getStartTime(), w.getEndTime());
        }
    }

    private void offerNext(com.college.booking.entity.Resource resource, java.time.LocalDate date,
                           java.time.LocalTime start, java.time.LocalTime end) {
        List<Waitlist> queue = waitlistRepository
                .findByResourceIdAndBookingDateAndStartTimeAndEndTimeAndStatusOrderByCreatedAtAsc(
                        resource.getId(), date, start, end, WaitlistStatus.WAITING);
        if (queue.isEmpty()) {
            return;
        }
        Waitlist next = queue.get(0);
        next.setStatus(WaitlistStatus.NOTIFIED);
        next.setNotifiedAt(Instant.now());
        next.setExpiresAt(Instant.now().plus(holdMinutes, ChronoUnit.MINUTES));
        waitlistRepository.save(next);
        notificationService.notify(next.getUser(), NotificationType.WAITLIST_AVAILABLE, "A slot opened",
                resource.getName() + " is available on " + date + " " + start + "–" + end
                        + ". Reserve it within " + holdMinutes + " minutes.",
                "/resources/" + resource.getId());
    }
}
