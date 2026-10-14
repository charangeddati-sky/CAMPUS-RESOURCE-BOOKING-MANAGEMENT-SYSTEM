package com.college.booking.controller;

import com.college.booking.dto.BookingDtos.ExamRequest;
import com.college.booking.dto.BookingDtos.EventRequest;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.EventExamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EventExamController {

    private final EventExamService eventExamService;

    public EventExamController(EventExamService eventExamService) {
        this.eventExamService = eventExamService;
    }

    @PostMapping("/events")
    public Map<String, Object> event(@Valid @RequestBody EventRequest request) {
        return eventExamService.createEvent(SecurityUtils.currentUser(), request);
    }

    @GetMapping("/events")
    public Object events() {
        return eventExamService.events();
    }

    @PostMapping("/exams")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> exam(@Valid @RequestBody ExamRequest request) {
        return eventExamService.createExam(SecurityUtils.currentUser(), request);
    }

    @GetMapping("/exams")
    public Object exams() {
        return eventExamService.exams();
    }
}
