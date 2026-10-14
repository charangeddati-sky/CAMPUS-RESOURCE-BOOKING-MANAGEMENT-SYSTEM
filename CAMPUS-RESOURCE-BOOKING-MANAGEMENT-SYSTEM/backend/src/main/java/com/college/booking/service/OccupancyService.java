package com.college.booking.service;

import com.college.booking.entity.Building;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceBlock;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.MaintenanceRepository;
import com.college.booking.repository.ResourceBlockRepository;
import com.college.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Computes campus-wide live status in a handful of queries instead of
 * one maintenance + block + conflict lookup per resource.
 */
@Service
public class OccupancyService {

    public static final Set<BookingStatus> ACTIVE = EnumSet.of(
            BookingStatus.PENDING_PROFESSOR,
            BookingStatus.PENDING_ADMIN,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private static final long TTL_MS = 15_000L;

    private final ResourceRepository resourceRepository;
    private final FloorRepository floorRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ResourceBlockRepository blockRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    public OccupancyService(ResourceRepository resourceRepository,
                            FloorRepository floorRepository,
                            MaintenanceRepository maintenanceRepository,
                            ResourceBlockRepository blockRepository,
                            BookingRepository bookingRepository,
                            Clock clock) {
        this.resourceRepository = resourceRepository;
        this.floorRepository = floorRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.blockRepository = blockRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot current() {
        CacheEntry hit = cache.get();
        long now = System.currentTimeMillis();
        if (hit != null && now - hit.at < TTL_MS) {
            return hit.snapshot;
        }
        synchronized (this) {
            hit = cache.get();
            now = System.currentTimeMillis();
            if (hit != null && now - hit.at < TTL_MS) {
                return hit.snapshot;
            }
            Snapshot snapshot = compute();
            cache.set(new CacheEntry(now, snapshot));
            return snapshot;
        }
    }

    public void invalidate() {
        cache.set(null);
    }

    public void invalidateAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate();
                }
            });
        } else {
            invalidate();
        }
    }

    private Snapshot compute() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<Resource> resources = resourceRepository.findAllEnabledDetailed();
        Set<Long> maint = new HashSet<>(maintenanceRepository.findResourceIdsActiveOn(today));
        List<ResourceBlock> blocks = blockRepository.findActiveOn(today);
        Set<Long> blockedNow = new HashSet<>();
        for (ResourceBlock block : blocks) {
            if (block.getStartTime() == null || block.getEndTime() == null) {
                blockedNow.add(block.getResource().getId());
            } else if (now.isBefore(block.getEndTime()) && now.plusMinutes(1).isAfter(block.getStartTime())) {
                blockedNow.add(block.getResource().getId());
            }
        }
        Map<Long, BookingStatus> occupied = new HashMap<>();
        for (Object[] row : bookingRepository.findOccupiedNow(today, now, ACTIVE)) {
            Long resourceId = (Long) row[0];
            BookingStatus status = (BookingStatus) row[1];
            BookingStatus existing = occupied.get(resourceId);
            if (existing == null || status == BookingStatus.PENDING_PROFESSOR || status == BookingStatus.PENDING_ADMIN) {
                occupied.put(resourceId, status);
            }
        }
        Map<Long, Long> floorCounts = new HashMap<>();
        for (Object[] row : floorRepository.countGroupedByBuilding()) {
            floorCounts.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, ResourceStatus> statusById = new HashMap<>();
        Map<Long, Integer> buildingTotal = new HashMap<>();
        Map<Long, Integer> buildingAvailable = new HashMap<>();
        Map<Long, Integer> floorTotal = new HashMap<>();
        Map<Long, Integer> floorAvailable = new HashMap<>();
        int available = 0, occupiedCount = 0, maintCount = 0, blockedCount = 0;

        for (Resource r : resources) {
            ResourceStatus live = classify(r, maint, blockedNow, occupied);
            statusById.put(r.getId(), live);
            Long buildingId = r.getBuilding().getId();
            Long floorId = r.getFloor().getId();
            buildingTotal.merge(buildingId, 1, Integer::sum);
            floorTotal.merge(floorId, 1, Integer::sum);
            switch (live) {
                case AVAILABLE -> {
                    available++;
                    buildingAvailable.merge(buildingId, 1, Integer::sum);
                    floorAvailable.merge(floorId, 1, Integer::sum);
                }
                case BOOKED, PENDING -> occupiedCount++;
                case MAINTENANCE -> maintCount++;
                case BLOCKED, OUT_OF_SERVICE -> blockedCount++;
                default -> available++;
            }
        }

        int total = Math.max(resources.size(), 1);
        Map<String, Object> live = new LinkedHashMap<>();
        live.put("available", available);
        live.put("occupied", occupiedCount);
        live.put("maintenance", maintCount);
        live.put("blocked", blockedCount);
        live.put("occupancyPercent", Math.round((occupiedCount / (double) total) * 100.0));
        live.put("asOf", Instant.now(clock).toString());

        return new Snapshot(Instant.now(clock), statusById, buildingTotal, buildingAvailable,
                floorTotal, floorAvailable, floorCounts, live, resources);
    }

    private ResourceStatus classify(Resource r, Set<Long> maint, Set<Long> blockedNow,
                                    Map<Long, BookingStatus> occupied) {
        if (!r.isEnabled() || r.getOperationalStatus() == ResourceStatus.OUT_OF_SERVICE) {
            return ResourceStatus.OUT_OF_SERVICE;
        }
        if (maint.contains(r.getId()) || r.getOperationalStatus() == ResourceStatus.MAINTENANCE) {
            return ResourceStatus.MAINTENANCE;
        }
        if (r.getOperationalStatus() == ResourceStatus.BLOCKED || blockedNow.contains(r.getId())) {
            return ResourceStatus.BLOCKED;
        }
        BookingStatus booking = occupied.get(r.getId());
        if (booking != null) {
            if (booking == BookingStatus.PENDING_PROFESSOR || booking == BookingStatus.PENDING_ADMIN) {
                return ResourceStatus.PENDING;
            }
            return ResourceStatus.BOOKED;
        }
        return ResourceStatus.AVAILABLE;
    }

    public Set<Long> busyResourceIds(Collection<Long> resourceIds, LocalDate date, LocalTime start, LocalTime end) {
        if (resourceIds == null || resourceIds.isEmpty() || date == null || start == null || end == null) {
            return Set.of();
        }
        Set<Long> busy = new HashSet<>(bookingRepository.findBusyResourceIds(resourceIds, date, start, end, ACTIVE));
        busy.addAll(maintenanceRepository.findResourceIdsActiveOnIn(date, resourceIds));
        List<ResourceBlock> blocks = blockRepository.findActiveOnIn(date, resourceIds);
        for (ResourceBlock block : blocks) {
            if (block.getStartTime() == null || block.getEndTime() == null) {
                busy.add(block.getResource().getId());
            } else if (start.isBefore(block.getEndTime()) && end.isAfter(block.getStartTime())) {
                busy.add(block.getResource().getId());
            }
        }
        return busy;
    }

    public record Snapshot(
            Instant generatedAt,
            Map<Long, ResourceStatus> statusById,
            Map<Long, Integer> buildingTotal,
            Map<Long, Integer> buildingAvailable,
            Map<Long, Integer> floorTotal,
            Map<Long, Integer> floorAvailable,
            Map<Long, Long> floorCounts,
            Map<String, Object> live,
            List<Resource> resources
    ) {
        public ResourceStatus status(Long resourceId) {
            return statusById.getOrDefault(resourceId, ResourceStatus.AVAILABLE);
        }

        public int availableInBuilding(Long buildingId) {
            return buildingAvailable.getOrDefault(buildingId, 0);
        }

        public int totalInBuilding(Long buildingId) {
            return buildingTotal.getOrDefault(buildingId, 0);
        }

        public int availableOnFloor(Long floorId) {
            return floorAvailable.getOrDefault(floorId, 0);
        }

        public int totalOnFloor(Long floorId) {
            return floorTotal.getOrDefault(floorId, 0);
        }

        public int floorsInBuilding(Long buildingId) {
            Long n = floorCounts.get(buildingId);
            return n == null ? 0 : n.intValue();
        }
    }

    private record CacheEntry(long at, Snapshot snapshot) {
    }
}
