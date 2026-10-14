package com.college.booking.controller;

import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.service.DashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final BuildingRepository buildingRepository;
    private final ResourceRepository resourceRepository;
    private final DashboardService dashboardService;
    private final String collegeName;
    private final String collegeShort;

    public PublicController(BuildingRepository buildingRepository,
                            ResourceRepository resourceRepository,
                            DashboardService dashboardService,
                            @Value("${app.college-name}") String collegeName,
                            @Value("${app.college-short}") String collegeShort) {
        this.buildingRepository = buildingRepository;
        this.resourceRepository = resourceRepository;
        this.dashboardService = dashboardService;
        this.collegeName = collegeName;
        this.collegeShort = collegeShort;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> live = dashboardService.liveCampus();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("college", collegeName);
        out.put("short", collegeShort);
        out.put("buildings", buildingRepository.count());
        out.put("resources", resourceRepository.countByEnabledTrue());
        out.put("availableNow", live.get("available"));
        out.put("occupied", live.get("occupied"));
        out.put("occupancyPercent", live.get("occupancyPercent"));
        return out;
    }
}
