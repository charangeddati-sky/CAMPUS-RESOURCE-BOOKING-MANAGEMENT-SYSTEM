package com.college.booking.controller;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.CheckInService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/{id}/check-in")
    public BookingView checkIn(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String token = body == null ? null : body.get("token");
        return checkInService.checkIn(id, SecurityUtils.currentUser(), token);
    }

    @PostMapping("/{id}/check-out")
    public BookingView checkOut(@PathVariable Long id) {
        return checkInService.checkOut(id, SecurityUtils.currentUser());
    }
}
