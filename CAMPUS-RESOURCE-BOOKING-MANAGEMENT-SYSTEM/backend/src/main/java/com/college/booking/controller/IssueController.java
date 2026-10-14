package com.college.booking.controller;

import com.college.booking.entity.Issue;
import com.college.booking.enums.IssueCategory;
import com.college.booking.enums.IssueStatus;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.OperationsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final OperationsService operationsService;

    public IssueController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @PostMapping
    public Issue report(@RequestBody Map<String, String> body) {
        return operationsService.reportIssue(
                SecurityUtils.currentUser(),
                Long.valueOf(body.get("resourceId")),
                IssueCategory.valueOf(body.get("category")),
                body.get("description")
        );
    }

    @GetMapping("/my")
    public List<Issue> mine() {
        return operationsService.myIssues(SecurityUtils.currentUserId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public List<Issue> all() {
        return operationsService.issues();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Issue update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        IssueStatus status = body.get("status") == null ? null : IssueStatus.valueOf(body.get("status"));
        Long assignee = body.get("assigneeId") == null ? null : Long.valueOf(body.get("assigneeId"));
        return operationsService.updateIssue(id, SecurityUtils.currentUser(), status, body.get("resolution"), assignee);
    }
}
