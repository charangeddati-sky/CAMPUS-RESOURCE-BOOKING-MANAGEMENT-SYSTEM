package com.college.booking.dto;

import com.college.booking.dto.CampusDtos.BuildingSummary;
import com.college.booking.dto.CampusDtos.ResourceCard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AnalyticsDtos {

    public record NamedCount(String name, long count, Long id) {
    }

    public record ResourceRank(
            Long resourceId,
            String name,
            String code,
            String building,
            String type,
            long bookings,
            long bookedHours,
            int utilizationPercent,
            Integer capacity
    ) {
    }

    public record HourCount(String hour, long count) {
    }

    public record DayCount(String date, String dayOfWeek, long count) {
    }

    public record PredictionPoint(
            String label,
            String value,
            String confidence,
            String basis
    ) {
    }

    public record DemandForecast(
            String date,
            String dayOfWeek,
            double expectedBookings,
            String confidence
    ) {
    }

    public record FrequentResourceForecast(
            Long resourceId,
            String name,
            String building,
            double expectedBookingsPerWeek,
            String confidence,
            String basis
    ) {
    }

    public record Predictions(
            String kind,
            String disclaimer,
            String method,
            String sampleFrom,
            String sampleTo,
            long sampleBookings,
            PredictionPoint peakHour,
            PredictionPoint peakDayOfWeek,
            List<FrequentResourceForecast> frequentResources,
            List<DemandForecast> nextSevenDays
    ) {
    }

    public record AnalyticsOverview(
            String from,
            String to,
            Instant generatedAt,
            Map<String, Object> kpis,
            Map<String, Object> live,
            List<DayCount> bookingTrends,
            List<HourCount> peakHours,
            List<NamedCount> peakDays,
            Map<String, Long> statusMix,
            List<ResourceRank> mostBooked,
            List<ResourceRank> leastBooked,
            List<ResourceRank> utilization,
            List<NamedCount> buildingPerformance,
            List<NamedCount> activeUsersByRole,
            List<Map<String, Object>> heatmap,
            Predictions predictions
    ) {
    }

    public record DashboardPayload(
            Map<String, Object> user,
            long pending,
            long confirmed,
            long completed,
            int noShows,
            Object upcoming,
            List<Object> recentBookings,
            Long pendingApprovals,
            Map<String, Object> live,
            List<Map<String, Object>> heatmap,
            List<BuildingSummary> buildings,
            List<ResourceCard> availableNow,
            List<DayCount> bookingTrends,
            List<HourCount> peakHours,
            Instant generatedAt
    ) {
    }
}
