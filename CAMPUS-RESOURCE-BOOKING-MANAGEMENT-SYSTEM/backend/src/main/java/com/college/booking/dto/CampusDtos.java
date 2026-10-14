package com.college.booking.dto;

import com.college.booking.enums.BuildingKind;
import com.college.booking.enums.ResourceKind;
import com.college.booking.enums.ResourceStatus;

import java.time.LocalTime;
import java.util.List;

public class CampusDtos {

    public record BuildingSummary(
            Long id,
            String name,
            String code,
            String virtueName,
            String description,
            String imageUrl,
            BuildingKind kind,
            boolean bookable,
            Double mapX,
            Double mapY,
            Double mapWidth,
            Double mapHeight,
            Double schematicX,
            Double schematicY,
            Double schematicWidth,
            Double schematicHeight,
            String department,
            int floors,
            int resources,
            int availableNow,
            String liveStatus
    ) {
    }

    public record BuildingDetail(
            BuildingSummary building,
            List<FloorSummary> floors
    ) {
    }

    public record FloorSummary(
            Long id,
            Long buildingId,
            String buildingName,
            String name,
            Integer level,
            String description,
            int classrooms,
            int labs,
            int halls,
            int libraries,
            int totalResources,
            int availableNow
    ) {
    }

    public record FloorMapDto(
            FloorSummary floor,
            List<ResourceCard> resources
    ) {
    }

    public record ResourceCard(
            Long id,
            String name,
            String code,
            String typeCode,
            String typeName,
            ResourceKind kind,
            Long buildingId,
            String buildingName,
            String buildingCode,
            Long floorId,
            String floorName,
            Integer floorLevel,
            Integer capacity,
            String department,
            ResourceStatus status,
            String description,
            String imageUrl,
            Double positionX,
            Double positionY,
            Double width,
            Double height,
            Double rotation,
            LocalTime workingHoursStart,
            LocalTime workingHoursEnd,
            String qrToken,
            Boolean projector,
            Boolean smartBoard,
            Boolean airConditioned,
            Boolean wifi,
            Boolean audio,
            Boolean microphones,
            Boolean stage,
            Integer computers,
            Integer studySeats,
            Boolean readingArea,
            String openingHours,
            String equipmentNotes,
            String softwareNotes,
            List<String> facilities,
            boolean favorite
    ) {
    }

    public record ResourceDetail(
            ResourceCard resource,
            List<HourSlot> timeline,
            BookingHistorySummary history
    ) {
    }

    public record HourSlot(
            String hour,
            String label,
            boolean available,
            String reason
    ) {
    }

    public record BookingHistorySummary(long total, long completed, long cancelled, long noShows) {
    }

    public record AvailabilityRequest(
            Long resourceId,
            String date,
            String startTime,
            String endTime
    ) {
    }

    public record AvailabilityResponse(
            boolean available,
            String status,
            String reason
    ) {
    }

    public record SearchRequest(
            String query,
            Long buildingId,
            Long floorId,
            String typeCode,
            Integer minCapacity,
            String department,
            String date,
            String startTime,
            String endTime,
            List<String> facilities
    ) {
    }

    public record LayoutUpdate(
            Long resourceId,
            Double positionX,
            Double positionY,
            Double width,
            Double height,
            Double rotation,
            String name,
            String typeCode
    ) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages) {
        public static <T> PageResponse<T> of(List<T> all, int page, int size) {
            int safeSize = Math.min(100, Math.max(1, size));
            int safePage = Math.max(0, page);
            int from = Math.min(safePage * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            int pages = all.isEmpty() ? 0 : (int) Math.ceil(all.size() / (double) safeSize);
            return new PageResponse<>(all.subList(from, to), safePage, safeSize, all.size(), pages);
        }
    }

    public record CampusOverview(
            List<BuildingSummary> buildings,
            java.util.Map<String, Object> live,
            java.util.List<java.util.Map<String, Object>> heatmap,
            java.time.Instant asOf
    ) {
    }
}
