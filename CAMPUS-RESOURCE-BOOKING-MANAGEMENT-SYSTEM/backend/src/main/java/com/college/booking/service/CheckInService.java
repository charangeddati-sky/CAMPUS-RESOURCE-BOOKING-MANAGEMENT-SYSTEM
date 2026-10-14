package com.college.booking.service;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.entity.Booking;
import com.college.booking.entity.CheckIn;
import com.college.booking.entity.User;
import com.college.booking.enums.BookingStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.CheckInRepository;
import com.college.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class CheckInService {

    private final BookingRepository bookingRepository;
    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final AuditService auditService;
    private final OccupancyService occupancyService;
    private final Clock clock;
    private final int graceMinutes;

    public CheckInService(BookingRepository bookingRepository,
                          CheckInRepository checkInRepository,
                          UserRepository userRepository,
                          BookingService bookingService,
                          AuditService auditService,
                          OccupancyService occupancyService,
                          Clock clock,
                          @Value("${app.no-show-grace-minutes}") int graceMinutes) {
        this.bookingRepository = bookingRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
        this.auditService = auditService;
        this.occupancyService = occupancyService;
        this.clock = clock;
        this.graceMinutes = graceMinutes;
    }

    @Transactional
    public BookingView checkIn(Long bookingId, User actor, String token) {
        Booking booking = bookingService.load(bookingId);
        if (!booking.getUser().getId().equals(actor.getId()) && actor.getRole() != com.college.booking.enums.Role.ADMIN) {
            throw ApiException.forbidden("You cannot check into another user's booking.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.badRequest("INVALID_STATUS", "Only confirmed bookings can be checked in.");
        }
        if (token != null && !token.isBlank() && booking.getCheckInToken() != null
                && !booking.getCheckInToken().equals(token)) {
            throw ApiException.forbidden("Invalid check-in token.");
        }
        if (!booking.getBookingDate().equals(LocalDate.now(clock))) {
            throw ApiException.badRequest("WRONG_DATE", "Check-in is only allowed on the booking date.");
        }
        LocalTime now = LocalTime.now(clock);
        if (now.isBefore(booking.getStartTime().minusMinutes(30))) {
            throw ApiException.badRequest("TOO_EARLY", "Check-in opens 30 minutes before the booking starts.");
        }
        if (now.isAfter(booking.getEndTime())) {
            throw ApiException.badRequest("TOO_LATE", "This booking has already ended.");
        }
        CheckIn checkIn = checkInRepository.findByBookingId(booking.getId()).orElseGet(CheckIn::new);
        checkIn.setBooking(booking);
        checkIn.setUser(actor);
        checkIn.setCheckedInAt(Instant.now());
        checkInRepository.save(checkIn);
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);
        occupancyService.invalidateAfterCommit();
        auditService.record(actor, "CHECK_IN", "Booking", booking.getId(), null);
        return bookingService.toView(booking);
    }

    @Transactional
    public BookingView checkOut(Long bookingId, User actor) {
        Booking booking = bookingService.load(bookingId);
        if (!booking.getUser().getId().equals(actor.getId()) && actor.getRole() != com.college.booking.enums.Role.ADMIN) {
            throw ApiException.forbidden("You cannot check out another user's booking.");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw ApiException.badRequest("INVALID_STATUS", "This booking is not checked in.");
        }
        CheckIn checkIn = checkInRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> ApiException.badRequest("NO_CHECKIN", "No check-in record found."));
        Instant out = Instant.now();
        checkIn.setCheckedOutAt(out);
        if (checkIn.getCheckedInAt() != null) {
            checkIn.setDurationMinutes(Duration.between(checkIn.getCheckedInAt(), out).toMinutes());
        }
        checkInRepository.save(checkIn);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        occupancyService.invalidateAfterCommit();
        auditService.record(actor, "CHECK_OUT", "Booking", booking.getId(),
                checkIn.getDurationMinutes() + " minutes");
        return bookingService.toView(booking);
    }

    @Transactional
    public int markNoShows() {
        LocalDate today = LocalDate.now(clock);
        LocalTime cutoff = LocalTime.now(clock).minusMinutes(graceMinutes);
        List<Booking> candidates = bookingRepository.findNoShowCandidates(BookingStatus.CONFIRMED, today, cutoff);
        int count = 0;
        for (Booking booking : candidates) {
            if (checkInRepository.findByBookingId(booking.getId()).isPresent()) {
                continue;
            }
            booking.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(booking);
            User user = booking.getUser();
            user.setNoShowCount(user.getNoShowCount() == null ? 1 : user.getNoShowCount() + 1);
            userRepository.save(user);
            count++;
        }
        if (count > 0) {
            occupancyService.invalidateAfterCommit();
        }
        return count;
    }
}
