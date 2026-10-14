package com.college.booking.controller;

import com.college.booking.dto.AuthDtos.AuthResponse;
import com.college.booking.dto.AuthDtos.LoginRequest;
import com.college.booking.dto.AuthDtos.RefreshRequest;
import com.college.booking.dto.AuthDtos.RegisterRequest;
import com.college.booking.dto.AuthDtos.UserResponse;
import com.college.booking.mapper.DtoMapper;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DtoMapper mapper;

    public AuthController(AuthService authService, DtoMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return mapper.toUser(SecurityUtils.currentUser());
    }
}
