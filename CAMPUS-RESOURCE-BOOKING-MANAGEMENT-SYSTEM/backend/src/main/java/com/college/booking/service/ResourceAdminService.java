package com.college.booking.service;

import com.college.booking.dto.CampusDtos.PageResponse;
import com.college.booking.dto.ResourceAdminDtos.AdminResourceView;
import com.college.booking.dto.ResourceAdminDtos.BuildingOpt;
import com.college.booking.dto.ResourceAdminDtos.BulkItemResult;
import com.college.booking.dto.ResourceAdminDtos.BulkRequest;
import com.college.booking.dto.ResourceAdminDtos.BulkResult;
import com.college.booking.dto.ResourceAdminDtos.FacilityOpt;
import com.college.booking.dto.ResourceAdminDtos.FloorOpt;
import com.college.booking.dto.ResourceAdminDtos.Lookups;
import com.college.booking.dto.ResourceAdminDtos.ResourceUpsertRequest;
import com.college.booking.dto.ResourceAdminDtos.TypeOpt;
import com.college.booking.entity.Building;
import com.college.booking.entity.Facility;
import com.college.booking.entity.Floor;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceFacility;
import com.college.booking.entity.ResourceType;
import com.college.booking.entity.User;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.FacilityRepository;
import com.college.booking.repository.FavoriteRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.IssueRepository;
import com.college.booking.repository.MaintenanceRepository;
import com.college.booking.repository.RecentlyVisitedRepository;
import com.college.booking.repository.ResourceBlockRepository;
import com.college.booking.repository.ResourceFacilityRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.ResourceTypeRepository;
import com.college.booking.repository.WaitlistRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ResourceAdminService {

    private static final Set<String> STATUSES = Set.of("AVAILABLE", "UNAVAILABLE", "MAINTENANCE", "INACTIVE");

    private final ResourceRepository resourceRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final FacilityRepository facilityRepository;
    private final ResourceFacilityRepository resourceFacilityRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final WaitlistRepository waitlistRepository;
    private final FavoriteRepository favoriteRepository;
    private final RecentlyVisitedRepository recentlyVisitedRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ResourceBlockRepository blockRepository;
    private final IssueRepository issueRepository;
    private final OccupancyService occupancyService;
    private final AuditService auditService;
    private final Clock clock;
    private final ResourceAdminService self;

    public ResourceAdminService(ResourceRepository resourceRepository,
                                ResourceTypeRepository resourceTypeRepository,
                                FloorRepository floorRepository,
                                BuildingRepository buildingRepository,
                                FacilityRepository facilityRepository,
                                ResourceFacilityRepository resourceFacilityRepository,
                                BookingResourceRepository bookingResourceRepository,
                                WaitlistRepository waitlistRepository,
                                FavoriteRepository favoriteRepository,
                                RecentlyVisitedRepository recentlyVisitedRepository,
                                MaintenanceRepository maintenanceRepository,
                                ResourceBlockRepository blockRepository,
                                IssueRepository issueRepository,
                                OccupancyService occupancyService,
                                AuditService auditService,
                                Clock clock,
                                @Lazy ResourceAdminService self) {
        this.resourceRepository = resourceRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.floorRepository = floorRepository;
        this.buildingRepository = buildingRepository;
        this.facilityRepository = facilityRepository;
        this.resourceFacilityRepository = resourceFacilityRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.waitlistRepository = waitlistRepository;
        this.favoriteRepository = favoriteRepository;
        this.recentlyVisitedRepository = recentlyVisitedRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.blockRepository = blockRepository;
        this.issueRepository = issueRepository;
        this.occupancyService = occupancyService;
        this.auditService = auditService;
        this.clock = clock;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public Lookups lookups() {
        List<BuildingOpt> buildings = buildingRepository.findAll().stream()
                .map(b -> new BuildingOpt(b.getId(), b.getName(), b.getCode(), b.isBookable()))
                .toList();
        List<FloorOpt> floors = floorRepository.findAllWithBuilding().stream()
                .map(f -> new FloorOpt(f.getId(), f.getBuilding().getId(), f.getBuilding().getName(), f.getName(), f.getLevel()))
                .toList();
        List<TypeOpt> types = resourceTypeRepository.findAll().stream()
                .map(t -> new TypeOpt(t.getCode(), t.getName(), t.getKind()))
                .toList();
        List<FacilityOpt> facilities = facilityRepository.findAll().stream()
                .map(f -> new FacilityOpt(f.getCode(), f.getName()))
                .toList();
        return new Lookups(buildings, floors, types, facilities);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminResourceView> list(String q, Long buildingId, Long floorId, String typeCode,
                                                String managementStatus, int page, int size) {
        Boolean enabled = null;
        String mgmt = normalizeStatus(managementStatus, false);
        if ("INACTIVE".equals(mgmt)) {
            enabled = false;
        } else if (mgmt != null) {
            enabled = true;
        }
        List<Resource> found = new ArrayList<>(resourceRepository.adminSearch(emptyToNull(q), buildingId, floorId, emptyToNull(typeCode), enabled));
        found.sort(java.util.Comparator.comparing((Resource r) -> r.getBuilding().getName())
                .thenComparing(r -> r.getFloor().getLevel())
                .thenComparing(Resource::getName));
        OccupancyService.Snapshot snap = occupancyService.current();
        if (mgmt != null && !"INACTIVE".equals(mgmt)) {
            found = found.stream().filter(r -> mgmt.equals(managementStatusOf(r))).toList();
        }
        List<Long> ids = found.stream().map(Resource::getId).toList();
        Map<Long, Long> upcoming = upcomingMap(ids);
        Map<Long, Long> totals = totalsMap(ids);
        Map<Long, List<String>> facilities = facilitiesMap(ids);
        List<AdminResourceView> views = found.stream()
                .map(r -> toView(r, snap, facilities.getOrDefault(r.getId(), List.of()),
                        upcoming.getOrDefault(r.getId(), 0L), totals.getOrDefault(r.getId(), 0L)))
                .toList();
        return PageResponse.of(views, page, size);
    }

    @Transactional(readOnly = true)
    public AdminResourceView get(Long id) {
        Resource r = resourceRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        OccupancyService.Snapshot snap = occupancyService.current();
        Map<Long, List<String>> fac = facilitiesMap(List.of(id));
        return toView(r, snap, fac.getOrDefault(id, List.of()),
                upcomingCount(id), bookingResourceRepository.countByResourceId(id));
    }

    @Transactional
    public AdminResourceView create(User admin, ResourceUpsertRequest req) {
        validate(req);
        Resource resource = new Resource();
        resource.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        resource.setPositionX(8d);
        resource.setPositionY(8d);
        resource.setWidth(16d);
        resource.setHeight(18d);
        apply(resource, req);
        resourceRepository.save(resource);
        replaceFacilities(resource, req.facilityCodes());
        occupancyService.invalidateAfterCommit();
        auditService.record(admin, "CREATE_RESOURCE", "Resource", resource.getId(), resource.getName());
        return get(resource.getId());
    }

    @Transactional
    public AdminResourceView update(User admin, Long id, ResourceUpsertRequest req) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        validate(req);
        String next = normalizeStatus(req.managementStatus(), true);
        if ("INACTIVE".equals(next) && resource.isEnabled()) {
            assertNoUpcoming(id, "This resource has upcoming bookings and cannot be deactivated.");
        }
        apply(resource, req);
        resourceRepository.save(resource);
        if (req.facilityCodes() != null) {
            replaceFacilities(resource, req.facilityCodes());
        }
        occupancyService.invalidateAfterCommit();
        auditService.record(admin, "UPDATE_RESOURCE", "Resource", id, resource.getName());
        return get(id);
    }

    @Transactional
    public AdminResourceView setStatus(User admin, Long id, String managementStatus) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        String next = normalizeStatus(managementStatus, true);
        if ("INACTIVE".equals(next) && resource.isEnabled()) {
            assertNoUpcoming(id, "This resource has upcoming bookings and cannot be deactivated. Cancel or complete those bookings first.");
        }
        applyManagementStatus(resource, next);
        resourceRepository.save(resource);
        occupancyService.invalidateAfterCommit();
        auditService.record(admin, "RESOURCE_STATUS", "Resource", id, next);
        return get(id);
    }

    @Transactional
    public void delete(User admin, Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        long bookings = bookingResourceRepository.countByResourceId(id);
        if (bookings > 0) {
            throw ApiException.conflict("HAS_BOOKING_HISTORY",
                    "Cannot delete " + resource.getName() + " because it has " + bookings
                            + " booking record(s). Deactivate it instead so history stays intact.");
        }
        waitlistRepository.deleteByResourceId(id);
        favoriteRepository.deleteByResourceId(id);
        recentlyVisitedRepository.deleteByResourceId(id);
        resourceFacilityRepository.deleteByResourceId(id);
        maintenanceRepository.deleteByResourceId(id);
        blockRepository.deleteByResourceId(id);
        issueRepository.deleteByResourceId(id);
        resourceRepository.delete(resource);
        occupancyService.invalidateAfterCommit();
        auditService.record(admin, "DELETE_RESOURCE", "Resource", id, resource.getName());
    }

    public BulkResult bulk(User admin, BulkRequest req) {
        if (req.ids() == null || req.ids().isEmpty()) {
            throw ApiException.badRequest("NO_IDS", "Select at least one resource.");
        }
        String action = req.action() == null ? "" : req.action().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DELETE", "DEACTIVATE", "INACTIVE", "ACTIVATE", "AVAILABLE", "MAINTENANCE", "UNAVAILABLE").contains(action)) {
            throw ApiException.badRequest("UNKNOWN_ACTION", "Unknown bulk action: " + action);
        }
        List<BulkItemResult> results = new ArrayList<>();
        int ok = 0;
        for (Long id : req.ids()) {
            String name = resourceRepository.findById(id).map(Resource::getName).orElse(null);
            try {
                switch (action) {
                    case "DELETE" -> self.delete(admin, id);
                    case "DEACTIVATE", "INACTIVE" -> self.setStatus(admin, id, "INACTIVE");
                    case "ACTIVATE", "AVAILABLE" -> self.setStatus(admin, id, "AVAILABLE");
                    case "MAINTENANCE" -> self.setStatus(admin, id, "MAINTENANCE");
                    case "UNAVAILABLE" -> self.setStatus(admin, id, "UNAVAILABLE");
                    default -> throw ApiException.badRequest("UNKNOWN_ACTION", "Unknown bulk action: " + action);
                }
                results.add(new BulkItemResult(id, name, true, "Updated"));
                ok++;
            } catch (ApiException ex) {
                results.add(new BulkItemResult(id, name, false, ex.getMessage()));
            }
        }
        return new BulkResult(ok, results.size() - ok, results);
    }

    private void validate(ResourceUpsertRequest req) {
        normalizeStatus(req.managementStatus(), true);
        if (req.capacity() != null && req.capacity() < 0) {
            throw ApiException.badRequest("INVALID_CAPACITY", "Capacity cannot be negative.");
        }
        if (req.workingHoursStart() != null && req.workingHoursEnd() != null
                && !req.workingHoursEnd().isAfter(req.workingHoursStart())) {
            throw ApiException.badRequest("INVALID_HOURS", "Working hours end must be after start.");
        }
        if (req.imageUrl() != null && !req.imageUrl().isBlank() && !isSafeUrl(req.imageUrl())) {
            throw ApiException.badRequest("INVALID_IMAGE", "Image must be an http(s) URL or a site-relative path.");
        }
    }

    private void apply(Resource resource, ResourceUpsertRequest req) {
        Floor floor = floorRepository.findById(req.floorId())
                .orElseThrow(() -> ApiException.notFound("Floor not found."));
        if (req.buildingId() != null && !floor.getBuilding().getId().equals(req.buildingId())) {
            throw ApiException.badRequest("FLOOR_MISMATCH", "That floor does not belong to the selected building.");
        }
        ResourceType type = resourceTypeRepository.findByCode(req.typeCode().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_TYPE", "Unknown resource type."));
        resource.setName(req.name().trim());
        String code = normalizeCode(req);
        Long currentId = resource.getId();
        if (currentId == null) {
            if (resourceRepository.findByCodeIgnoreCase(code).isPresent()) {
                throw ApiException.conflict("DUPLICATE_CODE", "A resource with code " + code + " already exists.");
            }
        } else if (resourceRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId)) {
            throw ApiException.conflict("DUPLICATE_CODE", "A resource with code " + code + " already exists.");
        }
        resource.setCode(code);
        resource.setResourceType(type);
        resource.setFloor(floor);
        resource.setBuilding(floor.getBuilding());
        resource.setCapacity(req.capacity());
        resource.setDepartment(blankToNull(req.department()));
        resource.setDescription(blankToNull(req.description()));
        resource.setImageUrl(blankToNull(req.imageUrl()));
        if (req.workingHoursStart() != null) {
            resource.setWorkingHoursStart(req.workingHoursStart());
        }
        if (req.workingHoursEnd() != null) {
            resource.setWorkingHoursEnd(req.workingHoursEnd());
        }
        resource.setProjector(req.projector());
        resource.setSmartBoard(req.smartBoard());
        resource.setAirConditioned(req.airConditioned());
        resource.setWifi(req.wifi());
        resource.setAudio(req.audio());
        resource.setMicrophones(req.microphones());
        resource.setStage(req.stage());
        resource.setComputers(req.computers());
        resource.setStudySeats(req.studySeats());
        resource.setReadingArea(req.readingArea());
        resource.setOpeningHours(blankToNull(req.openingHours()));
        resource.setEquipmentNotes(blankToNull(req.equipmentNotes()));
        resource.setSoftwareNotes(blankToNull(req.softwareNotes()));
        applyManagementStatus(resource, normalizeStatus(req.managementStatus(), true));
    }

    private void applyManagementStatus(Resource resource, String status) {
        switch (status) {
            case "AVAILABLE" -> {
                resource.setEnabled(true);
                resource.setOperationalStatus(ResourceStatus.AVAILABLE);
            }
            case "UNAVAILABLE" -> {
                resource.setEnabled(true);
                resource.setOperationalStatus(ResourceStatus.BLOCKED);
            }
            case "MAINTENANCE" -> {
                resource.setEnabled(true);
                resource.setOperationalStatus(ResourceStatus.MAINTENANCE);
            }
            case "INACTIVE" -> {
                resource.setEnabled(false);
                resource.setOperationalStatus(ResourceStatus.OUT_OF_SERVICE);
            }
            default -> throw ApiException.badRequest("INVALID_STATUS", "Status must be Available, Unavailable, Maintenance, or Inactive.");
        }
    }

    private void replaceFacilities(Resource resource, List<String> codes) {
        resourceFacilityRepository.deleteByResourceId(resource.getId());
        resourceFacilityRepository.flush();
        if (codes == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (String raw : codes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            if (!seen.add(code)) {
                continue;
            }
            Facility facility = facilityRepository.findByCode(code)
                    .orElseThrow(() -> ApiException.badRequest("UNKNOWN_FACILITY", "Unknown facility: " + code));
            ResourceFacility rf = new ResourceFacility();
            rf.setResource(resource);
            rf.setFacility(facility);
            resourceFacilityRepository.save(rf);
        }
    }

    private void assertNoUpcoming(Long resourceId, String message) {
        long n = upcomingCount(resourceId);
        if (n > 0) {
            throw ApiException.conflict("ACTIVE_BOOKINGS", message + " (" + n + " upcoming).");
        }
    }

    private long upcomingCount(Long resourceId) {
        return bookingResourceRepository.countUpcoming(
                resourceId, OccupancyService.ACTIVE, LocalDate.now(clock), LocalTime.now(clock));
    }

    private Map<Long, Long> upcomingMap(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Object[] row : bookingResourceRepository.countUpcomingGrouped(
                ids, OccupancyService.ACTIVE, LocalDate.now(clock), LocalTime.now(clock))) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, Long> totalsMap(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Object[] row : bookingResourceRepository.countAllGrouped(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, List<String>> facilitiesMap(List<Long> ids) {
        Map<Long, List<String>> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (ResourceFacility rf : resourceFacilityRepository.findByResourceIdIn(ids)) {
            map.computeIfAbsent(rf.getResource().getId(), k -> new ArrayList<>()).add(rf.getFacility().getCode());
        }
        return map;
    }

    private AdminResourceView toView(Resource r, OccupancyService.Snapshot snap, List<String> facilities,
                                     long upcoming, long total) {
        ResourceStatus live = r.isEnabled() ? snap.status(r.getId()) : ResourceStatus.OUT_OF_SERVICE;
        return new AdminResourceView(
                r.getId(), r.getName(), r.getCode(),
                r.getResourceType().getCode(), r.getResourceType().getName(), r.getResourceType().getKind(),
                r.getBuilding().getId(), r.getBuilding().getName(),
                r.getFloor().getId(), r.getFloor().getName(), r.getFloor().getLevel(),
                r.getCapacity(), r.getDepartment(), r.getDescription(), r.getImageUrl(),
                r.isEnabled(), r.getOperationalStatus(), managementStatusOf(r), live,
                r.getWorkingHoursStart(), r.getWorkingHoursEnd(),
                facilities,
                r.getProjector(), r.getSmartBoard(), r.getAirConditioned(), r.getWifi(),
                r.getAudio(), r.getMicrophones(), r.getStage(), r.getComputers(),
                r.getStudySeats(), r.getReadingArea(), r.getOpeningHours(),
                r.getEquipmentNotes(), r.getSoftwareNotes(),
                upcoming, total
        );
    }

    private String managementStatusOf(Resource r) {
        if (!r.isEnabled() || r.getOperationalStatus() == ResourceStatus.OUT_OF_SERVICE) {
            return "INACTIVE";
        }
        return switch (r.getOperationalStatus()) {
            case MAINTENANCE -> "MAINTENANCE";
            case BLOCKED -> "UNAVAILABLE";
            default -> "AVAILABLE";
        };
    }

    private String normalizeStatus(String raw, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw ApiException.badRequest("INVALID_STATUS", "Choose Available, Unavailable, Maintenance, or Inactive.");
            }
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("DISABLED".equals(value) || "OUT_OF_SERVICE".equals(value)) {
            value = "INACTIVE";
        }
        if ("BLOCKED".equals(value)) {
            value = "UNAVAILABLE";
        }
        if (!STATUSES.contains(value)) {
            throw ApiException.badRequest("INVALID_STATUS", "Status must be Available, Unavailable, Maintenance, or Inactive.");
        }
        return value;
    }

    private String normalizeCode(ResourceUpsertRequest req) {
        if (req.code() != null && !req.code().isBlank()) {
            return req.code().trim().toUpperCase(Locale.ROOT).replace(' ', '-');
        }
        String base = req.name().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        return base + "-" + (System.currentTimeMillis() % 10000);
    }

    private boolean isSafeUrl(String url) {
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.startsWith("https://") || u.startsWith("http://") || u.startsWith("/");
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
