package com.college.booking.dto;

import com.college.booking.enums.ApprovalStatus;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.RecurrenceType;
import com.college.booking.enums.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class BookingDtos {

    public record CreateBookingRequest(
            @NotEmpty List<Long> resourceIds,
            List<EquipmentQty> equipment,
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Size(max = 160) String title,
            @Size(max = 2000) String purpose,
            Integer attendees,
            String requirements,
            RecurrenceType recurrenceType,
            LocalDate recurrenceEndDate,
            String bookingKind
    ) {
    }

    public record EquipmentQty(Long equipmentId, Integer quantity) {
    }

    public record DecisionRequest(String comment) {
    }

    public record BookingView(
            Long id,
            String title,
            String purpose,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            BookingStatus status,
            Integer attendees,
            String requirements,
            RecurrenceType recurrenceType,
            String bookingKind,
            String rejectionReason,
            @JsonIgnore String checkInToken,
            Long userId,
            String userName,
            String userEmail,
            Role userRole,
            List<ResourceBrief> resources,
            List<ApprovalView> approvals,
            Instant createdAt
    ) {
    }

    public record CalendarEvent(
            Long id,
            String title,
            String start,
            String end,
            String status,
            String color,
            String url,
            String resourceName,
            String buildingName
    ) {
    }

    public record ResourceBrief(
            Long id,
            String name,
            String code,
            String typeName,
            String buildingName,
            String floorName
    ) {
    }

    public record ApprovalView(
            Long id,
            Role requiredRole,
            ApprovalStatus status,
            String comment,
            String approverName,
            Instant decidedAt
    ) {
    }

    public record EventRequest(
            @NotNull String name,
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            Integer expectedAttendees,
            List<Long> resourceIds,
            String requiredEquipment,
            String description
    ) {
    }

    public record ExamRequest(
            @NotNull String name,
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotNull Integer requiredCapacity
    ) {
    }

    public record WaitlistRequest(
            @NotNull Long resourceId,
            @NotNull LocalDate date,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            Integer attendees,
            String purpose
    ) {
    }
}
