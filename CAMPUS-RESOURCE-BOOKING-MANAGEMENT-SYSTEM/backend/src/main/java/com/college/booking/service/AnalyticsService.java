package com.college.booking.service;

import com.college.booking.dto.AnalyticsDtos.AnalyticsOverview;
import com.college.booking.dto.AnalyticsDtos.DayCount;
import com.college.booking.dto.AnalyticsDtos.DemandForecast;
import com.college.booking.dto.AnalyticsDtos.FrequentResourceForecast;
import com.college.booking.dto.AnalyticsDtos.HourCount;
import com.college.booking.dto.AnalyticsDtos.NamedCount;
import com.college.booking.dto.AnalyticsDtos.PredictionPoint;
import com.college.booking.dto.AnalyticsDtos.Predictions;
import com.college.booking.dto.AnalyticsDtos.ResourceRank;
import com.college.booking.entity.Booking;
import com.college.booking.entity.BookingResource;
import com.college.booking.entity.Building;
import com.college.booking.entity.Resource;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.IssueStatus;
import com.college.booking.enums.Role;
import com.college.booking.repository.BookingApprovalRepository;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.IssueRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.UserRepository;
import com.college.booking.enums.ApprovalStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Set<BookingStatus> COUNTED = EnumSet.complementOf(
            EnumSet.of(BookingStatus.CANCELLED, BookingStatus.REJECTED));
    private static final Set<BookingStatus> EXCLUDED = EnumSet.of(BookingStatus.CANCELLED, BookingStatus.REJECTED);

    private final BookingRepository bookingRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final ResourceRepository resourceRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final BookingApprovalRepository approvalRepository;
    private final OccupancyService occupancyService;
    private final Clock clock;

    public AnalyticsService(BookingRepository bookingRepository,
                            BookingResourceRepository bookingResourceRepository,
                            ResourceRepository resourceRepository,
                            BuildingRepository buildingRepository,
                            FloorRepository floorRepository,
                            UserRepository userRepository,
                            IssueRepository issueRepository,
                            BookingApprovalRepository approvalRepository,
                            OccupancyService occupancyService,
                            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.resourceRepository = resourceRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.approvalRepository = approvalRepository;
        this.occupancyService = occupancyService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverview overview(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        if (from == null) {
            from = today.minusDays(29);
        }
        if (to == null) {
            to = today;
        }
        if (to.isBefore(from)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        long spanDays = Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);
        double availableHoursPerResource = spanDays * 10.0;

        List<BookingResource> rows = bookingResourceRepository.findDetailedBetween(from, to);
        List<Resource> resources = occupancyService.current().resources();
        if (resources.isEmpty()) {
            resources = resourceRepository.findAllEnabledDetailed();
        }

        Map<String, Long> statusMix = new LinkedHashMap<>();
        for (BookingStatus s : BookingStatus.values()) {
            statusMix.put(s.name(), 0L);
        }
        for (Object[] row : bookingRepository.countStatusBetween(from, to)) {
            statusMix.put(((BookingStatus) row[0]).name(), (Long) row[1]);
        }

        Map<LocalDate, Long> byDate = new HashMap<>();
        for (Object[] row : bookingRepository.countByDateBetween(from, to)) {
            byDate.put((LocalDate) row[0], (Long) row[1]);
        }
        List<DayCount> trends = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            trends.add(new DayCount(d.toString(), d.getDayOfWeek().name(), byDate.getOrDefault(d, 0L)));
        }

        Map<Integer, Long> hours = new HashMap<>();
        Map<DayOfWeek, Long> weekdays = new EnumMap<>(DayOfWeek.class);
        Map<Long, Agg> byResource = new HashMap<>();
        Map<Long, Long> byBuilding = new HashMap<>();
        Map<Long, Booking> seenBookings = new HashMap<>();
        for (BookingResource br : rows) {
            Booking b = br.getBooking();
            seenBookings.putIfAbsent(b.getId(), b);
            if (!COUNTED.contains(b.getStatus())) {
                continue;
            }
            int hour = b.getStartTime().getHour();
            hours.merge(hour, 1L, Long::sum);
            weekdays.merge(b.getBookingDate().getDayOfWeek(), 1L, Long::sum);
            long bookedHours = Math.max(1, Duration.between(b.getStartTime(), b.getEndTime()).toHours());
            Resource r = br.getResource();
            Agg agg = byResource.computeIfAbsent(r.getId(), id -> new Agg(r));
            agg.bookings++;
            agg.bookedHours += bookedHours;
            byBuilding.merge(r.getBuilding().getId(), 1L, Long::sum);
        }

        List<HourCount> peakHours = new ArrayList<>();
        for (int h = 8; h <= 17; h++) {
            peakHours.add(new HourCount(String.format("%02d:00", h), hours.getOrDefault(h, 0L)));
        }
        List<NamedCount> peakDays = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            peakDays.add(new NamedCount(title(dow.name()), weekdays.getOrDefault(dow, 0L), null));
        }

        List<ResourceRank> ranks = new ArrayList<>();
        for (Resource r : resources) {
            Agg agg = byResource.getOrDefault(r.getId(), new Agg(r));
            int util = (int) Math.min(100, Math.round((agg.bookedHours / availableHoursPerResource) * 100.0));
            ranks.add(new ResourceRank(
                    r.getId(), r.getName(), r.getCode(),
                    r.getBuilding().getName(), r.getResourceType().getName(),
                    agg.bookings, agg.bookedHours, util, r.getCapacity()
            ));
        }
        List<ResourceRank> most = ranks.stream()
                .sorted(Comparator.comparingLong(ResourceRank::bookings).reversed())
                .limit(10)
                .toList();
        List<ResourceRank> least = ranks.stream()
                .sorted(Comparator.comparingLong(ResourceRank::bookings).thenComparing(ResourceRank::name))
                .limit(10)
                .toList();
        List<ResourceRank> utilization = ranks.stream()
                .sorted(Comparator.comparingInt(ResourceRank::utilizationPercent).reversed())
                .limit(20)
                .toList();

        Map<Long, String> buildingNames = buildingRepository.findAll().stream()
                .collect(Collectors.toMap(Building::getId, Building::getName));
        List<NamedCount> buildingPerformance = buildingNames.entrySet().stream()
                .map(e -> new NamedCount(e.getValue(), byBuilding.getOrDefault(e.getKey(), 0L), e.getKey()))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed())
                .toList();

        List<NamedCount> usersByRole = new ArrayList<>();
        for (Object[] row : bookingRepository.countActiveUsersByRole(from, to, EXCLUDED)) {
            usersByRole.add(new NamedCount(((Role) row[0]).name(), (Long) row[1], null));
        }

        OccupancyService.Snapshot snap = occupancyService.current();
        Map<String, Object> kpis = new LinkedHashMap<>();
        long totalBookings = statusMix.values().stream().mapToLong(Long::longValue).sum();
        long completed = statusMix.getOrDefault("COMPLETED", 0L);
        long cancelled = statusMix.getOrDefault("CANCELLED", 0L);
        long noShow = statusMix.getOrDefault("NO_SHOW", 0L);
        kpis.put("totalBookings", totalBookings);
        kpis.put("completed", completed);
        kpis.put("cancelled", cancelled);
        kpis.put("noShows", noShow);
        kpis.put("activeUsers", bookingRepository.countActiveUsersBetween(from, to, EXCLUDED));
        kpis.put("totalResources", snap.resources().size());
        kpis.put("totalBuildings", buildingRepository.count());
        kpis.put("totalFloors", floorRepository.count());
        kpis.put("availableNow", snap.live().get("available"));
        kpis.put("occupancyPercent", snap.live().get("occupancyPercent"));
        kpis.put("pendingApprovals",
                approvalRepository.countByRequiredRoleAndStatus(Role.ADMIN, ApprovalStatus.PENDING)
                        + approvalRepository.countByRequiredRoleAndStatus(Role.PROFESSOR, ApprovalStatus.PENDING));
        kpis.put("openIssues", issueRepository.countByStatusIn(
                List.of(IssueStatus.REPORTED, IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS)));
        kpis.put("avgUtilizationPercent", ranks.isEmpty() ? 0 :
                Math.round(ranks.stream().mapToInt(ResourceRank::utilizationPercent).average().orElse(0)));
        kpis.put("cancellationRatePercent", totalBookings == 0 ? 0 : Math.round(cancelled * 100.0 / totalBookings));
        kpis.put("noShowRatePercent", totalBookings == 0 ? 0 : Math.round(noShow * 100.0 / totalBookings));
        kpis.put("totalUsers", userRepository.count());

        Predictions predictions = predict(from, to, rows.size(), hours, weekdays, ranks, today);

        return new AnalyticsOverview(
                from.toString(), to.toString(), Instant.now(clock),
                kpis, snap.live(), trends, peakHours, peakDays, statusMix,
                most, least, utilization, buildingPerformance, usersByRole,
                heatmap(snap), predictions
        );
    }

    public List<Map<String, Object>> heatmap(OccupancyService.Snapshot snap) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Building b : buildingRepository.findAll()) {
            int total = snap.totalInBuilding(b.getId());
            int available = snap.availableInBuilding(b.getId());
            int usedPct = total == 0 ? 0 : (int) Math.round(((total - available) / (double) total) * 100);
            String color = usedPct >= 75 ? "HIGH" : usedPct >= 40 ? "MEDIUM" : "LOW";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("buildingId", b.getId());
            row.put("name", b.getName());
            row.put("code", b.getCode());
            row.put("percent", usedPct);
            row.put("level", color);
            row.put("available", available);
            row.put("total", total);
            rows.add(row);
        }
        return rows;
    }

    public List<DayCount> trendStrip(int days) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(days - 1L);
        Map<LocalDate, Long> byDate = new HashMap<>();
        for (Object[] row : bookingRepository.countByDateBetween(from, to)) {
            byDate.put((LocalDate) row[0], (Long) row[1]);
        }
        List<DayCount> trends = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            trends.add(new DayCount(d.toString(), d.getDayOfWeek().name(), byDate.getOrDefault(d, 0L)));
        }
        return trends;
    }

    public List<HourCount> peakHourStrip(int days) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(days - 1L);
        Map<Integer, Long> hours = new HashMap<>();
        for (BookingResource br : bookingResourceRepository.findDetailedBetween(from, to)) {
            Booking b = br.getBooking();
            if (COUNTED.contains(b.getStatus())) {
                hours.merge(b.getStartTime().getHour(), 1L, Long::sum);
            }
        }
        List<HourCount> peakHours = new ArrayList<>();
        for (int h = 8; h <= 17; h++) {
            peakHours.add(new HourCount(String.format("%02d:00", h), hours.getOrDefault(h, 0L)));
        }
        return peakHours;
    }

    private Predictions predict(LocalDate from, LocalDate to, long sampleRows,
                                Map<Integer, Long> hours, Map<DayOfWeek, Long> weekdays,
                                List<ResourceRank> ranks, LocalDate today) {
        long weeks = Math.max(1, ChronoUnit.WEEKS.between(from, to) + 1);
        int peakH = hours.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(10);
        long peakHCount = hours.getOrDefault(peakH, 0L);
        DayOfWeek peakD = weekdays.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(DayOfWeek.MONDAY);
        long peakDCount = weekdays.getOrDefault(peakD, 0L);

        List<FrequentResourceForecast> frequent = ranks.stream()
                .filter(r -> r.bookings() > 0)
                .sorted(Comparator.comparingLong(ResourceRank::bookings).reversed())
                .limit(8)
                .map(r -> new FrequentResourceForecast(
                        r.resourceId(), r.name(), r.building(),
                        round1(r.bookings() / (double) weeks),
                        confidence(r.bookings(), weeks),
                        r.bookings() + " bookings in the selected window"
                ))
                .toList();

        Map<DayOfWeek, Long> weekdayBookings = new EnumMap<>(DayOfWeek.class);
        weekdayBookings.putAll(weekdays);
        long weekdayOccurrences = Math.max(1, weeks);
        List<DemandForecast> next = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            LocalDate d = today.plusDays(i);
            double expected = weekdayBookings.getOrDefault(d.getDayOfWeek(), 0L) / (double) weekdayOccurrences;
            next.add(new DemandForecast(d.toString(), title(d.getDayOfWeek().name()),
                    round1(expected), expected >= 2 ? "MEDIUM" : "LOW"));
        }

        return new Predictions(
                "FORECAST",
                "Predictions are statistical forecasts from historical bookings. They are not live occupancy and do not reserve rooms.",
                "Weekday and hour-of-day averages over the selected history window. Confidence grows with sample size.",
                from.toString(), to.toString(), sampleRows,
                new PredictionPoint("Peak hour", String.format("%02d:00", peakH),
                        confidence(peakHCount, weeks), peakHCount + " historical starts in this hour"),
                new PredictionPoint("Peak day", title(peakD.name()),
                        confidence(peakDCount, weeks), peakDCount + " historical bookings on this weekday"),
                frequent, next
        );
    }

    private String confidence(long events, long weeks) {
        if (events >= 12 && weeks >= 3) {
            return "HIGH";
        }
        if (events >= 4) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String title(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static final class Agg {
        final Resource resource;
        long bookings;
        long bookedHours;

        Agg(Resource resource) {
            this.resource = resource;
        }
    }
}
