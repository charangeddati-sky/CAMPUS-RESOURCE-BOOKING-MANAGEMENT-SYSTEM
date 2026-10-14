package com.college.booking.dto;

import com.college.booking.dto.BookingDtos.BookingView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class AiDtos {

    public record InterpretRequest(
            @NotBlank @Size(max = 2000) String prompt
    ) {
    }

    public record ChatRequest(
            @NotBlank @Size(max = 2000) String message,
            List<ChatTurn> history
    ) {
    }

    public record ChatTurn(String role, String content) {
    }

    public record Intent(
            String resourceType,
            Integer capacity,
            String date,
            String startTime,
            String endTime,
            List<String> requiredFacilities,
            String building,
            String query,
            String intent,
            String resourceName
    ) {
    }

    public record Recommendation(
            Long resourceId,
            String resourceName,
            String resourceCode,
            int score,
            String reason,
            boolean available
    ) {
    }

    public record InterpretResponse(
            Intent intent,
            List<Recommendation> recommendations,
            String explanation,
            boolean aiAvailable
    ) {
    }

    public record ChatResponse(
            String reply,
            List<Map<String, Object>> data,
            boolean aiAvailable
    ) {
    }

    public record InsightsRequest(@NotBlank @Size(max = 2000) String question) {
    }

    public record BookByLanguageRequest(
            @NotBlank @Size(max = 2000) String prompt,
            Boolean confirm
    ) {
    }

    public record BookByLanguageResponse(
            String action,
            String message,
            List<String> questions,
            Intent intent,
            List<Recommendation> matches,
            BookingView booking,
            boolean aiAvailable
    ) {
    }
}
