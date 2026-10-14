package com.college.booking.controller;

import com.college.booking.dto.CampusDtos.AvailabilityResponse;
import com.college.booking.dto.CampusDtos.PageResponse;
import com.college.booking.dto.CampusDtos.ResourceCard;
import com.college.booking.dto.CampusDtos.ResourceDetail;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.CampusService;
import com.college.booking.service.OperationsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final CampusService campusService;
    private final OperationsService operationsService;

    public ResourceController(CampusService campusService, OperationsService operationsService) {
        this.campusService = campusService;
        this.operationsService = operationsService;
    }

    @GetMapping
    public PageResponse<ResourceCard> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalTime startTime,
            @RequestParam(required = false) LocalTime endTime,
            @RequestParam(required = false) List<String> facilities,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return campusService.search(q, buildingId, floorId, typeCode, department, minCapacity,
                date, startTime, endTime, facilities, SecurityUtils.currentUser(), page, size);
    }

    @GetMapping("/{id}")
    public ResourceDetail get(@PathVariable Long id, @RequestParam(required = false) LocalDate date) {
        return campusService.resource(id, SecurityUtils.currentUser(), date);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long id,
            @RequestParam LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        return campusService.availability(id, date, startTime, endTime);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Object update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationsService.updateResource(id, body);
    }
}
