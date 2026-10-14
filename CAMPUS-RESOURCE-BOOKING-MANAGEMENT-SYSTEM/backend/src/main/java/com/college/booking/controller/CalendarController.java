package com.college.booking.controller;

import com.college.booking.dto.BookingDtos.CalendarEvent;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final BookingService bookingService;

    public CalendarController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/events")
    public List<CalendarEvent> events(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate end = to == null ? LocalDate.now().plusMonths(2) : to;
        return bookingService.calendarEvents(SecurityUtils.currentUser(), start, end);
    }
}
