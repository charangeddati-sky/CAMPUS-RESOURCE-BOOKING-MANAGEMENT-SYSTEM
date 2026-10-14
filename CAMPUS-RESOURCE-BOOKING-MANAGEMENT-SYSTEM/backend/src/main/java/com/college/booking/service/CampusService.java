package com.college.booking.service;

import com.college.booking.dto.CampusDtos.AvailabilityResponse;
import com.college.booking.dto.CampusDtos.BookingHistorySummary;
import com.college.booking.dto.CampusDtos.BuildingDetail;
import com.college.booking.dto.CampusDtos.BuildingSummary;
import com.college.booking.dto.CampusDtos.CampusOverview;
import com.college.booking.dto.CampusDtos.FloorMapDto;
import com.college.booking.dto.CampusDtos.FloorSummary;
import com.college.booking.dto.CampusDtos.HourSlot;
import com.college.booking.dto.CampusDtos.LayoutUpdate;
import com.college.booking.dto.CampusDtos.PageResponse;
import com.college.booking.dto.CampusDtos.ResourceCard;
import com.college.booking.dto.CampusDtos.ResourceDetail;
import com.college.booking.entity.Building;
import com.college.booking.entity.Favorite;
import com.college.booking.entity.Floor;
import com.college.booking.entity.RecentlyVisited;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceFacility;
import com.college.booking.entity.ResourceType;
import com.college.booking.entity.User;
import com.college.booking.enums.ResourceKind;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.exception.ApiException;
import com.college.booking.mapper.DtoMapper;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.FavoriteRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.RecentlyVisitedRepository;
import com.college.booking.repository.ResourceFacilityRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.ResourceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CampusService {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceFacilityRepository resourceFacilityRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final FavoriteRepository favoriteRepository;
    private final RecentlyVisitedRepository recentlyVisitedRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final AvailabilityService availabilityService;
    private final OccupancyService occupancyService;
    private final AnalyticsService analyticsService;
    private final DtoMapper mapper;

    public CampusService(BuildingRepository buildingRepository, FloorRepository floorRepository,
                         ResourceRepository resourceRepository, ResourceFacilityRepository resourceFacilityRepository,
                         ResourceTypeRepository resourceTypeRepository, FavoriteRepository favoriteRepository,
                         RecentlyVisitedRepository recentlyVisitedRepository,
                         BookingResourceRepository bookingResourceRepository,
                         AvailabilityService availabilityService, OccupancyService occupancyService,
                         AnalyticsService analyticsService, DtoMapper mapper) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFacilityRepository = resourceFacilityRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.favoriteRepository = favoriteRepository;
        this.recentlyVisitedRepository = recentlyVisitedRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.availabilityService = availabilityService;
        this.occupancyService = occupancyService;
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BuildingSummary> campus(User user) {
        return campusFromSnapshot(occupancyService.current());
    }

    public List<BuildingSummary> campusFromSnapshot(OccupancyService.Snapshot snap) {
        return buildingRepository.findAll().stream()
                .sorted(Comparator.comparing(Building::getId))
                .map(b -> mapper.toBuilding(b, snap.floorsInBuilding(b.getId()),
                        snap.totalInBuilding(b.getId()), snap.availableInBuilding(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampusOverview overview(User user) {
        OccupancyService.Snapshot snap = occupancyService.current();
        return new CampusOverview(campusFromSnapshot(snap), snap.live(), analyticsService.heatmap(snap), snap.generatedAt());
    }

    @Transactional(readOnly = true)
    public BuildingDetail building(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Building not found."));
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> resources = resourceRepository.findEnabledDetailedByBuildingId(id);
        Map<Long, List<Resource>> byFloor = resources.stream().collect(Collectors.groupingBy(r -> r.getFloor().getId()));
        List<FloorSummary> floors = floorRepository.findByBuildingIdOrderByLevelAsc(id).stream()
                .map(floor -> toFloorSummary(floor, byFloor.getOrDefault(floor.getId(), List.of()), snap))
                .toList();
        return new BuildingDetail(mapper.toBuilding(building, snap.floorsInBuilding(id),
                snap.totalInBuilding(id), snap.availableInBuilding(id)), floors);
    }

    @Transactional(readOnly = true)
    public FloorMapDto floor(Long id, User user) {
        Floor floor = floorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Floor not found."));
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> resources = resourceRepository.findEnabledDetailedByFloorId(id);
        List<ResourceCard> cards = toCards(resources, user, snap);
        return new FloorMapDto(toFloorSummary(floor, resources, snap), cards);
    }

    @Transactional(readOnly = true)
    public List<FloorSummary> allFloors() {
        OccupancyService.Snapshot snap = occupancyService.current();
        Map<Long, List<Resource>> byFloor = snap.resources().stream()
                .collect(Collectors.groupingBy(r -> r.getFloor().getId()));
        return floorRepository.findAllWithBuilding().stream()
                .map(floor -> toFloorSummary(floor, byFloor.getOrDefault(floor.getId(), List.of()), snap))
                .toList();
    }

    @Transactional
    public ResourceDetail resource(Long id, User user, LocalDate date) {
        Resource resource = resourceRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("That resource does not exist."));
        if (user != null) {
            RecentlyVisited visit = recentlyVisitedRepository.findByUserIdAndResourceId(user.getId(), id)
                    .orElseGet(RecentlyVisited::new);
            visit.setUser(user);
            visit.setResource(resource);
            visit.setVisitedAt(Instant.now());
            recentlyVisitedRepository.save(visit);
        }
        LocalDate target = date == null ? LocalDate.now() : date;
        List<HourSlot> timeline = availabilityService.timeline(resource, target);
        Map<String, Long> mix = new HashMap<>();
        for (Object[] row : bookingResourceRepository.countStatusByResource(id)) {
            mix.put(row[0].toString(), (Long) row[1]);
        }
        long total = mix.values().stream().mapToLong(Long::longValue).sum();
        return new ResourceDetail(
                toCards(List.of(resource), user, occupancyService.current()).get(0),
                timeline,
                new BookingHistorySummary(
                        total,
                        mix.getOrDefault("COMPLETED", 0L),
                        mix.getOrDefault("CANCELLED", 0L),
                        mix.getOrDefault("NO_SHOW", 0L)
                )
        );
    }

    public AvailabilityResponse availability(Long resourceId, LocalDate date, LocalTime start, LocalTime end) {
        return availabilityService.check(resourceId, date, start, end);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceCard> search(String q, Long buildingId, Long floorId, String typeCode,
                                             String department, Integer minCapacity, LocalDate date,
                                             LocalTime start, LocalTime end, List<String> facilities, User user,
                                             int page, int size) {
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> found = resourceRepository.search(emptyToNull(q), buildingId, floorId, emptyToNull(typeCode),
                emptyToNull(department), minCapacity);
        if (facilities != null && !facilities.isEmpty()) {
            Map<Long, Set<String>> have = facilitiesByResource(found.stream().map(Resource::getId).toList());
            Set<String> need = facilities.stream().map(f -> f.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            found = found.stream().filter(r -> {
                Set<String> codes = have.getOrDefault(r.getId(), Set.of());
                return need.stream().allMatch(n -> codes.stream().anyMatch(c -> c.equalsIgnoreCase(n)));
            }).toList();
        }
        if (date != null && start != null && end != null && !found.isEmpty()) {
            Set<Long> busy = occupancyService.busyResourceIds(
                    found.stream().map(Resource::getId).toList(), date, start, end);
            found = found.stream().filter(r -> {
                if (!r.getBuilding().isBookable() || !r.isEnabled()
                        || r.getOperationalStatus() == ResourceStatus.OUT_OF_SERVICE
                        || r.getOperationalStatus() == ResourceStatus.BLOCKED) {
                    return false;
                }
                return !busy.contains(r.getId());
            }).toList();
        }
        List<ResourceCard> cards = toCards(found, user, snap);
        return PageResponse.of(cards, page, size);
    }

    public List<ResourceCard> search(String q, Long buildingId, Long floorId, String typeCode,
                                     String department, Integer minCapacity, LocalDate date,
                                     LocalTime start, LocalTime end, List<String> facilities, User user) {
        return search(q, buildingId, floorId, typeCode, department, minCapacity, date, start, end, facilities, user,
                0, 100).items();
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceCard> availableNow(Long buildingId, Long floorId, String typeCode, String q,
                                                   User user, int page, int size) {
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> filtered = snap.resources().stream()
                .filter(r -> snap.status(r.getId()) == ResourceStatus.AVAILABLE)
                .filter(r -> buildingId == null || r.getBuilding().getId().equals(buildingId))
                .filter(r -> floorId == null || r.getFloor().getId().equals(floorId))
                .filter(r -> typeCode == null || typeCode.isBlank() || r.getResourceType().getCode().equalsIgnoreCase(typeCode))
                .filter(r -> q == null || q.isBlank()
                        || r.getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        || r.getCode().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        || r.getBuilding().getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(r -> r.getBuilding().getName() + r.getName()))
                .toList();
        return PageResponse.of(toCards(filtered, user, snap), page, size);
    }

    public List<ResourceCard> availableNow(Long buildingId, Long floorId, String typeCode, User user) {
        return availableNow(buildingId, floorId, typeCode, null, user, 0, 100).items();
    }

    public List<ResourceCard> availableNowPreview(int limit, User user, OccupancyService.Snapshot snap) {
        List<Resource> filtered = snap.resources().stream()
                .filter(r -> snap.status(r.getId()) == ResourceStatus.AVAILABLE)
                .sorted(Comparator.comparing(Resource::getName))
                .limit(limit)
                .toList();
        return toCards(filtered, user, snap);
    }

    public ResourceCard byQr(String token, User user) {
        Resource resource = resourceRepository.findByQrToken(token)
                .orElseThrow(() -> ApiException.notFound("Unknown QR code."));
        return toCards(List.of(resource), user, occupancyService.current()).get(0);
    }

    @Transactional
    public void toggleFavorite(User user, Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ApiException.notFound("Resource not found."));
        favoriteRepository.findByUserIdAndResourceId(user.getId(), resourceId).ifPresentOrElse(
                favoriteRepository::delete,
                () -> {
                    Favorite f = new Favorite();
                    f.setUser(user);
                    f.setResource(resource);
                    favoriteRepository.save(f);
                }
        );
    }

    @Transactional(readOnly = true)
    public List<ResourceCard> favorites(User user) {
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> resources = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getResource)
                .toList();
        return toCards(resources, user, snap);
    }

    @Transactional(readOnly = true)
    public List<ResourceCard> recent(User user) {
        OccupancyService.Snapshot snap = occupancyService.current();
        List<Resource> resources = recentlyVisitedRepository.findTop8ByUserIdOrderByVisitedAtDesc(user.getId()).stream()
                .map(RecentlyVisited::getResource)
                .toList();
        return toCards(resources, user, snap);
    }

    @Transactional
    public void saveLayout(Long floorId, List<LayoutUpdate> updates) {
        Floor floor = floorRepository.findById(floorId).orElseThrow(() -> ApiException.notFound("Floor not found."));
        for (LayoutUpdate u : updates) {
            Resource resource;
            if (u.resourceId() == null) {
                resource = new Resource();
                resource.setFloor(floor);
                resource.setBuilding(floor.getBuilding());
                resource.setQrToken(UUID.randomUUID().toString().replace("-", ""));
                resource.setEnabled(true);
                resource.setOperationalStatus(ResourceStatus.AVAILABLE);
                resource.setCapacity(30);
                resource.setWorkingHoursStart(LocalTime.of(8, 0));
                resource.setWorkingHoursEnd(LocalTime.of(18, 0));
            } else {
                resource = resourceRepository.findById(u.resourceId())
                        .orElseThrow(() -> ApiException.notFound("Resource not found."));
            }
            if (u.name() != null) {
                resource.setName(u.name());
                if (resource.getCode() == null) {
                    resource.setCode(u.name().replace(" ", "-").toUpperCase() + "-" + System.currentTimeMillis() % 10000);
                }
            }
            if (u.typeCode() != null) {
                ResourceType type = resourceTypeRepository.findByCode(u.typeCode())
                        .orElseThrow(() -> ApiException.notFound("Unknown resource type."));
                resource.setResourceType(type);
            } else if (resource.getResourceType() == null) {
                resource.setResourceType(resourceTypeRepository.findByCode("CLASSROOM").orElseThrow());
            }
            if (u.positionX() != null) resource.setPositionX(u.positionX());
            if (u.positionY() != null) resource.setPositionY(u.positionY());
            if (u.width() != null) resource.setWidth(u.width());
            if (u.height() != null) resource.setHeight(u.height());
            if (u.rotation() != null) resource.setRotation(u.rotation());
            resourceRepository.save(resource);
        }
        occupancyService.invalidateAfterCommit();
    }

    public List<ResourceType> types() {
        return resourceTypeRepository.findAll();
    }

    private FloorSummary toFloorSummary(Floor floor, List<Resource> resources, OccupancyService.Snapshot snap) {
        int classrooms = (int) resources.stream().filter(r -> r.getResourceType().getKind() == ResourceKind.CLASSROOM).count();
        int labs = (int) resources.stream().filter(r -> r.getResourceType().getKind() == ResourceKind.LABORATORY).count();
        int halls = (int) resources.stream().filter(r ->
                r.getResourceType().getKind() == ResourceKind.SEMINAR_HALL
                        || r.getResourceType().getKind() == ResourceKind.AUDITORIUM
                        || r.getResourceType().getKind() == ResourceKind.EXAMINATION_HALL).count();
        int libraries = (int) resources.stream().filter(r -> r.getResourceType().getKind() == ResourceKind.LIBRARY).count();
        return mapper.toFloor(floor, classrooms, labs, halls, libraries, resources.size(), snap.availableOnFloor(floor.getId()));
    }

    private List<ResourceCard> toCards(List<Resource> resources, User user, OccupancyService.Snapshot snap) {
        if (resources.isEmpty()) {
            return List.of();
        }
        List<Long> ids = resources.stream().map(Resource::getId).toList();
        Map<Long, List<ResourceFacility>> fac = resourceFacilityRepository.findByResourceIdIn(ids).stream()
                .collect(Collectors.groupingBy(rf -> rf.getResource().getId()));
        Set<Long> favs = user == null ? Set.of() : favoriteRepository.findResourceIdsByUserId(user.getId());
        List<ResourceCard> cards = new ArrayList<>();
        for (Resource r : resources) {
            cards.add(mapper.toResource(
                    r,
                    fac.getOrDefault(r.getId(), List.of()),
                    favs.contains(r.getId()),
                    snap.status(r.getId())
            ));
        }
        return cards;
    }

    private Map<Long, Set<String>> facilitiesByResource(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> map = new HashMap<>();
        for (ResourceFacility rf : resourceFacilityRepository.findByResourceIdIn(ids)) {
            map.computeIfAbsent(rf.getResource().getId(), k -> new HashSet<>()).add(rf.getFacility().getCode());
        }
        return map;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
