package com.college.booking.controller;

import com.college.booking.dto.CampusDtos.PageResponse;
import com.college.booking.dto.ResourceAdminDtos.AdminResourceView;
import com.college.booking.dto.ResourceAdminDtos.BulkRequest;
import com.college.booking.dto.ResourceAdminDtos.BulkResult;
import com.college.booking.dto.ResourceAdminDtos.Lookups;
import com.college.booking.dto.ResourceAdminDtos.ResourceUpsertRequest;
import com.college.booking.dto.ResourceAdminDtos.StatusRequest;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.ResourceAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resources")
@PreAuthorize("hasRole('ADMIN')")
public class ResourceAdminController {

    private final ResourceAdminService resourceAdminService;

    public ResourceAdminController(ResourceAdminService resourceAdminService) {
        this.resourceAdminService = resourceAdminService;
    }

    @GetMapping("/lookups")
    public Lookups lookups() {
        return resourceAdminService.lookups();
    }

    @GetMapping
    public PageResponse<AdminResourceView> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return resourceAdminService.list(q, buildingId, floorId, typeCode, status, page, size);
    }

    @GetMapping("/{id}")
    public AdminResourceView get(@PathVariable Long id) {
        return resourceAdminService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResourceView create(@Valid @RequestBody ResourceUpsertRequest request) {
        return resourceAdminService.create(SecurityUtils.currentUser(), request);
    }

    @PutMapping("/{id}")
    public AdminResourceView update(@PathVariable Long id, @Valid @RequestBody ResourceUpsertRequest request) {
        return resourceAdminService.update(SecurityUtils.currentUser(), id, request);
    }

    @PatchMapping("/{id}/status")
    public AdminResourceView status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return resourceAdminService.setStatus(SecurityUtils.currentUser(), id, request.managementStatus());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        resourceAdminService.delete(SecurityUtils.currentUser(), id);
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return resourceAdminService.bulk(SecurityUtils.currentUser(), request);
    }
}
