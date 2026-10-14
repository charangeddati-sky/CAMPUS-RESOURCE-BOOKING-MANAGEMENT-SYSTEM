package com.college.booking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @Value("${app.college-name}")
    private String collegeName;

    @Value("${app.college-short}")
    private String collegeShort;

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "college", collegeName,
                "short", collegeShort,
                "product", "CampusOS"
        );
    }
}
