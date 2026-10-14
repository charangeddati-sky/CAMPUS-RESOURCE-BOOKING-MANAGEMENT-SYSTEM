package com.college.booking.controller;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.dto.BookingDtos.CreateBookingRequest;
import com.college.booking.dto.BookingDtos.WaitlistRequest;
import com.college.booking.entity.Waitlist;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.BookingService;
import com.college.booking.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final WaitlistService waitlistService;

    public BookingController(BookingService bookingService, WaitlistService waitlistService) {
        this.bookingService = bookingService;
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public BookingView create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(SecurityUtils.currentUser(), request);
    }

    @GetMapping("/my")
    public List<BookingView> mine() {
        return bookingService.mine(SecurityUtils.currentUser());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public List<BookingView> all() {
        return bookingService.all();
    }

    @GetMapping("/{id}")
    public BookingView get(@PathVariable Long id) {
        return bookingService.get(id, SecurityUtils.currentUser());
    }

    @PostMapping("/{id}/cancel")
    public BookingView cancel(@PathVariable Long id) {
        return bookingService.cancel(id, SecurityUtils.currentUser());
    }

    @GetMapping("/{id}/calendar")
    public ResponseEntity<byte[]> calendar(@PathVariable Long id) {
        String ics = bookingService.ics(id, SecurityUtils.currentUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=booking-" + id + ".ics")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/waitlist")
    public Waitlist waitlist(@Valid @RequestBody WaitlistRequest request) {
        return waitlistService.join(SecurityUtils.currentUser(), request);
    }

    @GetMapping("/waitlist/my")
    public List<Waitlist> myWaitlist() {
        return waitlistService.mine(SecurityUtils.currentUserId());
    }
}
