package com.college.booking.dto;

import com.college.booking.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            Role role,
            String department,
            String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMs,
            UserResponse user
    ) {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            Role role,
            String department,
            String phone,
            Integer noShowCount
    ) {
    }
}
