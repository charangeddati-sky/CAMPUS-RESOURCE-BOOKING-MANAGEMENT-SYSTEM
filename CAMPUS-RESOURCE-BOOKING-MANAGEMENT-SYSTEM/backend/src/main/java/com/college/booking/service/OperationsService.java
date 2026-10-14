package com.college.booking.service;

import com.college.booking.entity.Equipment;
import com.college.booking.entity.Issue;
import com.college.booking.entity.Maintenance;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceBlock;
import com.college.booking.entity.User;
import com.college.booking.enums.IssueCategory;
import com.college.booking.enums.IssueStatus;
import com.college.booking.enums.NotificationType;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.EquipmentRepository;
import com.college.booking.repository.IssueRepository;
import com.college.booking.repository.MaintenanceRepository;
import com.college.booking.repository.ResourceBlockRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class OperationsService {

    private final IssueRepository issueRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ResourceBlockRepository blockRepository;
    private final ResourceRepository resourceRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OccupancyService occupancyService;

    public OperationsService(IssueRepository issueRepository, MaintenanceRepository maintenanceRepository,
            ResourceBlockRepository blockRepository, ResourceRepository resourceRepository,
            EquipmentRepository equipmentRepository, UserRepository userRepository,
            NotificationService notificationService, AuditService auditService,
            OccupancyService occupancyService) {
        this.issueRepository = issueRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.blockRepository = blockRepository;
        this.resourceRepository = resourceRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.occupancyService = occupancyService;
    }

    @Transactional
    public Issue reportIssue(User reporter, Long resourceId, IssueCategory category, String description) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        Issue issue = new Issue();
        issue.setResource(resource);
        issue.setReporter(reporter);
        issue.setCategory(category);
        issue.setDescription(description);
        issue.setStatus(IssueStatus.REPORTED);
        issueRepository.save(issue);
        userRepository.findByRole(com.college.booking.enums.Role.ADMIN)
                .forEach(admin -> notificationService.notify(admin, NotificationType.ISSUE_UPDATE, "New issue reported",
                        reporter.getFullName() + " reported " + category + " on " + resource.getName(),
                        "/admin/issues"));
        auditService.record(reporter, "REPORT_ISSUE", "Issue", issue.getId(), category.name());
        return issue;
    }

    @Transactional
    public Issue updateIssue(Long id, User actor, IssueStatus status, String resolution, Long assigneeId) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found."));
        if (status != null)
            issue.setStatus(status);
        if (resolution != null)
            issue.setResolution(resolution);
        if (assigneeId != null) {
            issue.setAssignee(
                    userRepository.findById(assigneeId).orElseThrow(() -> ApiException.notFound("User not found.")));
            if (issue.getStatus() == IssueStatus.REPORTED)
                issue.setStatus(IssueStatus.ASSIGNED);
        }
        if (status == IssueStatus.RESOLVED) {
            issue.setResolvedAt(Instant.now());
        }
        issueRepository.save(issue);
        notificationService.notify(issue.getReporter(), NotificationType.ISSUE_UPDATE, "Issue update",
                issue.getResource().getName() + " issue is now "
                        + issue.getStatus().name().toLowerCase().replace('_', ' '),
                "/issues");
        return issue;
    }

    public List<Issue> issues() {
        return issueRepository.findAll();
    }

    public List<Issue> myIssues(Long userId) {
        return issueRepository.findByReporterIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Maintenance createMaintenance(User admin, Long resourceId, LocalDate start, LocalDate end, String reason) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        Maintenance m = new Maintenance();
        m.setResource(resource);
        m.setStartDate(start);
        m.setEndDate(end);
        m.setReason(reason);
        m.setActive(true);
        maintenanceRepository.save(m);
        resource.setOperationalStatus(ResourceStatus.MAINTENANCE);
        resourceRepository.save(resource);
        occupancyService.invalidateAfterCommit();
        auditService.record(admin, "CREATE_MAINTENANCE", "Maintenance", m.getId(), resource.getName());
        return m;
    }

    public List<Maintenance> maintenance() {
        return maintenanceRepository.findAll();
    }

    @Transactional
    public ResourceBlock block(User admin, Long resourceId, LocalDate start, LocalDate end,
            LocalTime startTime, LocalTime endTime, String reason) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        ResourceBlock block = new ResourceBlock();
        block.setResource(resource);
        block.setStartDate(start);
        block.setEndDate(end);
        block.setStartTime(startTime);
        block.setEndTime(endTime);
        block.setReason(reason);
        block.setActive(true);
        blockRepository.save(block);
        occupancyService.invalidateAfterCommit();
        return block;
    }

    public List<Equipment> equipment() {
        return equipmentRepository.findAll();
    }

    @Transactional
    public Equipment saveEquipment(Map<String, Object> body) {
        Equipment e = body.get("id") == null ? new Equipment()
                : equipmentRepository.findById(Long.valueOf(body.get("id").toString()))
                        .orElseThrow(() -> ApiException.notFound("Equipment not found."));
        if (body.get("name") != null)
            e.setName(body.get("name").toString());
        if (body.get("type") != null)
            e.setType(body.get("type").toString());
        if (body.get("quantity") != null)
            e.setQuantity(Integer.valueOf(body.get("quantity").toString()));
        if (body.get("available") != null)
            e.setAvailable(Integer.valueOf(body.get("available").toString()));
        if (body.get("description") != null)
            e.setDescription(body.get("description").toString());
        if (e.getAvailable() == null)
            e.setAvailable(e.getQuantity());
        e.setEnabled(true);
        return equipmentRepository.save(e);
    }

    @Transactional
    public Resource updateResource(Long id, Map<String, Object> body) {
        Resource r = resourceRepository.findById(id).orElseThrow(() -> ApiException.notFound("Resource not found."));
        if (body.get("name") != null)
            r.setName(body.get("name").toString());
        if (body.get("capacity") != null)
            r.setCapacity(Integer.valueOf(body.get("capacity").toString()));
        if (body.get("department") != null)
            r.setDepartment(body.get("department").toString());
        if (body.get("description") != null)
            r.setDescription(body.get("description").toString());
        if (body.get("enabled") != null)
            r.setEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        if (body.get("operationalStatus") != null) {
            r.setOperationalStatus(ResourceStatus.valueOf(body.get("operationalStatus").toString()));
        }
        if (body.get("workingHoursStart") != null)
            r.setWorkingHoursStart(LocalTime.parse(body.get("workingHoursStart").toString()));
        if (body.get("workingHoursEnd") != null)
            r.setWorkingHoursEnd(LocalTime.parse(body.get("workingHoursEnd").toString()));
        occupancyService.invalidateAfterCommit();
        return resourceRepository.save(r);
    }

    public List<User> users() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, Map<String, Object> body) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (body.get("enabled") != null)
            u.setEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        if (body.get("department") != null)
            u.setDepartment(body.get("department").toString());
        if (body.get("fullName") != null)
            u.setFullName(body.get("fullName").toString());
        return userRepository.save(u);
    }
}
