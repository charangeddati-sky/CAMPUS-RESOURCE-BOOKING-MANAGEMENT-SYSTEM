package com.college.booking.controller;

import com.college.booking.dto.AnalyticsDtos.AnalyticsOverview;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.AnalyticsService;
import com.college.booking.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;

    public DashboardController(DashboardService dashboardService, AnalyticsService analyticsService) {
        this.dashboardService = dashboardService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return dashboardService.forUser(SecurityUtils.currentUser());
    }

    @GetMapping("/analytics/heatmap")
    public List<Map<String, Object>> heatmap() {
        return dashboardService.heatmap();
    }

    @GetMapping("/analytics/utilization")
    public List<Map<String, Object>> utilization(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return dashboardService.utilization(from, to);
    }

    @GetMapping("/analytics/live")
    public Map<String, Object> live() {
        return dashboardService.liveCampus();
    }

    @GetMapping("/analytics/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public AnalyticsOverview overview(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return analyticsService.overview(from, to);
    }

    @GetMapping("/analytics/predictions")
    @PreAuthorize("hasRole('ADMIN')")
    public Object predictions(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return analyticsService.overview(from, to).predictions();
    }
}
