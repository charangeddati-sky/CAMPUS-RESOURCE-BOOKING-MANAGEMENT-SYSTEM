package com.college.booking.controller;

import com.college.booking.dto.CampusDtos.BuildingDetail;
import com.college.booking.dto.CampusDtos.BuildingSummary;
import com.college.booking.dto.CampusDtos.CampusOverview;
import com.college.booking.dto.CampusDtos.FloorMapDto;
import com.college.booking.dto.CampusDtos.FloorSummary;
import com.college.booking.dto.CampusDtos.LayoutUpdate;
import com.college.booking.dto.CampusDtos.PageResponse;
import com.college.booking.dto.CampusDtos.ResourceCard;
import com.college.booking.entity.ResourceType;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.CampusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping("/campus")
    public List<BuildingSummary> campus() {
        return campusService.campus(SecurityUtils.currentUser());
    }

    @GetMapping("/campus/overview")
    public CampusOverview campusOverview() {
        return campusService.overview(SecurityUtils.currentUser());
    }

    @GetMapping("/floors")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FloorSummary> allFloors() {
        return campusService.allFloors();
    }

    @GetMapping("/buildings")
    public List<BuildingSummary> buildings() {
        return campusService.campus(SecurityUtils.currentUser());
    }

    @GetMapping("/buildings/{id}")
    public BuildingDetail building(@PathVariable Long id) {
        return campusService.building(id);
    }

    @GetMapping("/buildings/{id}/floors")
    public BuildingDetail floors(@PathVariable Long id) {
        return campusService.building(id);
    }

    @GetMapping("/floors/{id}")
    public FloorMapDto floor(@PathVariable Long id) {
        return campusService.floor(id, SecurityUtils.currentUser());
    }

    @GetMapping("/floors/{id}/resources")
    public FloorMapDto floorResources(@PathVariable Long id) {
        return campusService.floor(id, SecurityUtils.currentUser());
    }

    @PutMapping("/admin/floors/{id}/layout")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> saveLayout(@PathVariable Long id, @RequestBody List<LayoutUpdate> updates) {
        campusService.saveLayout(id, updates);
        return Map.of("status", "saved");
    }

    @GetMapping("/resource-types")
    public List<ResourceType> types() {
        return campusService.types();
    }

    @GetMapping("/available-now")
    public PageResponse<ResourceCard> availableNow(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return campusService.availableNow(buildingId, floorId, typeCode, q, SecurityUtils.currentUser(), page, size);
    }
}
