package com.college.booking.dto;

import com.college.booking.enums.ResourceKind;
import com.college.booking.enums.ResourceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public class ResourceAdminDtos {

    public record ResourceUpsertRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 40) String code,
            @NotBlank String typeCode,
            Long buildingId,
            @NotNull Long floorId,
            @NotNull @Min(0) @Max(5000) Integer capacity,
            @Size(max = 80) String department,
            @Size(max = 2000) String description,
            @Size(max = 400) String imageUrl,
            @NotBlank String managementStatus,
            LocalTime workingHoursStart,
            LocalTime workingHoursEnd,
            List<String> facilityCodes,
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
            @Size(max = 80) String openingHours,
            @Size(max = 400) String equipmentNotes,
            @Size(max = 400) String softwareNotes
    ) {
    }

    public record StatusRequest(@NotBlank String managementStatus) {
    }

    public record BulkRequest(
            @NotBlank String action,
            @NotNull List<Long> ids
    ) {
    }

    public record BulkItemResult(Long id, String name, boolean ok, String message) {
    }

    public record BulkResult(int succeeded, int failed, List<BulkItemResult> results) {
    }

    public record AdminResourceView(
            Long id,
            String name,
            String code,
            String typeCode,
            String typeName,
            ResourceKind kind,
            Long buildingId,
            String buildingName,
            Long floorId,
            String floorName,
            Integer floorLevel,
            Integer capacity,
            String department,
            String description,
            String imageUrl,
            boolean enabled,
            ResourceStatus operationalStatus,
            String managementStatus,
            ResourceStatus liveStatus,
            LocalTime workingHoursStart,
            LocalTime workingHoursEnd,
            List<String> facilities,
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
            long upcomingBookings,
            long totalBookings
    ) {
    }

    public record Lookups(
            List<BuildingOpt> buildings,
            List<FloorOpt> floors,
            List<TypeOpt> types,
            List<FacilityOpt> facilities
    ) {
    }

    public record BuildingOpt(Long id, String name, String code, boolean bookable) {
    }

    public record FloorOpt(Long id, Long buildingId, String buildingName, String name, Integer level) {
    }

    public record TypeOpt(String code, String name, ResourceKind kind) {
    }

    public record FacilityOpt(String code, String name) {
    }
}
