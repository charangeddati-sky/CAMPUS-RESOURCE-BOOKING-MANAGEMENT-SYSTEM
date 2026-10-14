package com.college.booking.service;

import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.entity.Booking;
import com.college.booking.entity.User;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.Role;
import com.college.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {

    private static final Set<BookingStatus> PENDING = EnumSet.of(
            BookingStatus.PENDING_PROFESSOR, BookingStatus.PENDING_ADMIN);
    private static final Set<BookingStatus> UPCOMING = EnumSet.of(
            BookingStatus.PENDING_PROFESSOR, BookingStatus.PENDING_ADMIN,
            BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    private final BookingRepository bookingRepository;
    private final OccupancyService occupancyService;
    private final AnalyticsService analyticsService;
    private final CampusService campusService;
    private final BookingService bookingService;
    private final Clock clock;

    public DashboardService(BookingRepository bookingRepository,
                            OccupancyService occupancyService,
                            AnalyticsService analyticsService,
                            CampusService campusService,
                            BookingService bookingService,
                            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.occupancyService = occupancyService;
        this.analyticsService = analyticsService;
        this.campusService = campusService;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> forUser(User user) {
        OccupancyService.Snapshot snap = occupancyService.current();
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", Map.of("id", user.getId(), "name", user.getFullName(), "role", user.getRole().name()));
        data.put("pending", bookingRepository.countByUserIdAndStatusIn(user.getId(), PENDING));
        data.put("confirmed", bookingRepository.countByUserIdAndStatusIn(user.getId(),
                EnumSet.of(BookingStatus.CONFIRMED)));
        data.put("completed", bookingRepository.countByUserIdAndStatusIn(user.getId(),
                EnumSet.of(BookingStatus.COMPLETED)));
        data.put("noShows", user.getNoShowCount() == null ? 0 : user.getNoShowCount());

        List<Booking> upcoming = bookingRepository.findUpcoming(user.getId(), UPCOMING, today, now);
        data.put("upcoming", upcoming.isEmpty() ? null : bookingService.toView(upcoming.get(0)));

        List<Booking> recent = bookingRepository.findRecentByUser(user.getId());
        data.put("recentBookings", recent.stream().limit(5).map(bookingService::toView).toList());

        if (user.getRole() == Role.PROFESSOR || user.getRole() == Role.ADMIN) {
            data.put("pendingApprovals", bookingService.pendingFor(user).size());
        }

        data.put("live", snap.live());
        data.put("heatmap", analyticsService.heatmap(snap));
        data.put("buildings", campusService.campusFromSnapshot(snap));
        data.put("availableNow", campusService.availableNowPreview(8, user, snap));
        data.put("generatedAt", Instant.now(clock).toString());

        if (user.getRole() == Role.ADMIN) {
            data.put("bookingTrends", analyticsService.trendStrip(14));
            data.put("peakHours", analyticsService.peakHourStrip(14));
            data.put("totalResources", snap.resources().size());
            data.put("availableNowCount", snap.live().get("available"));
            data.put("occupancyPercent", snap.live().get("occupancyPercent"));
            data.put("bookingsToday", bookingRepository.countByBookingDate(today));
        }
        return data;
    }

    public Map<String, Object> adminStats() {
        return analyticsService.overview(null, null).kpis();
    }

    public Map<String, Object> liveCampus() {
        return occupancyService.current().live();
    }

    public List<Map<String, Object>> heatmap() {
        return analyticsService.heatmap(occupancyService.current());
    }

    public List<Map<String, Object>> utilization(LocalDate from, LocalDate to) {
        return analyticsService.overview(from, to).utilization().stream()
                .map(r -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("resourceId", r.resourceId());
                    row.put("name", r.name());
                    row.put("building", r.building());
                    row.put("type", r.type());
                    row.put("bookedHours", r.bookedHours());
                    row.put("utilization", r.utilizationPercent());
                    return row;
                })
                .toList();
    }

    public BookingView upcoming(User user) {
        Object value = forUser(user).get("upcoming");
        return value instanceof BookingView view ? view : null;
    }
}
