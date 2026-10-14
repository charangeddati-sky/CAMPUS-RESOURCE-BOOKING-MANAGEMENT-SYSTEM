package com.college.booking.controller;

import com.college.booking.entity.Equipment;
import com.college.booking.entity.Maintenance;
import com.college.booking.entity.ResourceBlock;
import com.college.booking.entity.User;
import com.college.booking.repository.AuditLogRepository;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.OperationsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OperationsService operationsService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(OperationsService operationsService, AuditLogRepository auditLogRepository) {
        this.operationsService = operationsService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/users")
    public List<User> users() {
        return operationsService.users();
    }

    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationsService.updateUser(id, body);
    }

    @GetMapping("/equipment")
    public List<Equipment> equipment() {
        return operationsService.equipment();
    }

    @PostMapping("/equipment")
    public Equipment saveEquipment(@RequestBody Map<String, Object> body) {
        return operationsService.saveEquipment(body);
    }

    @GetMapping("/maintenance")
    public List<Maintenance> maintenance() {
        return operationsService.maintenance();
    }

    @PostMapping("/maintenance")
    public Maintenance createMaintenance(@RequestBody Map<String, String> body) {
        return operationsService.createMaintenance(
                SecurityUtils.currentUser(),
                Long.valueOf(body.get("resourceId")),
                LocalDate.parse(body.get("startDate")),
                LocalDate.parse(body.get("endDate")),
                body.get("reason")
        );
    }

    @PostMapping("/blocks")
    public ResourceBlock block(@RequestBody Map<String, String> body) {
        return operationsService.block(
                SecurityUtils.currentUser(),
                Long.valueOf(body.get("resourceId")),
                LocalDate.parse(body.get("startDate")),
                LocalDate.parse(body.get("endDate")),
                body.get("startTime") == null ? null : LocalTime.parse(body.get("startTime")),
                body.get("endTime") == null ? null : LocalTime.parse(body.get("endTime")),
                body.get("reason")
        );
    }

    @GetMapping("/audit")
    public Object audit(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "40") int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
}
