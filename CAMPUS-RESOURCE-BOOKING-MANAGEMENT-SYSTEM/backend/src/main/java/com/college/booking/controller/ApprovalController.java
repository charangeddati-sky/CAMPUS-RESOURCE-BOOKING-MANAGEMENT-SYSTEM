package com.college.booking.controller;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.dto.BookingDtos.DecisionRequest;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApprovalController {

    private final BookingService bookingService;

    public ApprovalController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/approvals")
    public List<BookingView> pending() {
        return bookingService.pendingFor(SecurityUtils.currentUser());
    }

    @PostMapping("/bookings/{id}/approve")
    public BookingView approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return bookingService.approve(id, SecurityUtils.currentUser(), request);
    }

    @PostMapping("/bookings/{id}/reject")
    public BookingView reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest request) {
        return bookingService.reject(id, SecurityUtils.currentUser(), request);
    }
}
