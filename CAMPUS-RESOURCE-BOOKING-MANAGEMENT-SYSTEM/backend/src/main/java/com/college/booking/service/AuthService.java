package com.college.booking.service;

import com.college.booking.dto.AuthDtos.AuthResponse;
import com.college.booking.dto.AuthDtos.LoginRequest;
import com.college.booking.dto.AuthDtos.RegisterRequest;
import com.college.booking.entity.User;
import com.college.booking.enums.Role;
import com.college.booking.exception.ApiException;
import com.college.booking.mapper.DtoMapper;
import com.college.booking.repository.UserRepository;
import com.college.booking.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final DtoMapper mapper;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       DtoMapper mapper, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("EMAIL_EXISTS", "An account with this email already exists.");
        }
        User user = new User();
        user.setFullName(req.fullName());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role() == null ? Role.STUDENT : req.role());
        user.setDepartment(req.department());
        user.setPhone(req.phone());
        user.setEnabled(true);
        user.setNoShowCount(0);
        userRepository.save(user);
        auditService.record(user, "REGISTER", "User", user.getId(), "Account created");
        return tokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password."));
        auditService.record(user, "LOGIN", "User", user.getId(), "Signed in");
        return tokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        try {
            var claims = jwtService.parse(refreshToken);
            if (!"refresh".equals(claims.get("typ"))) {
                throw ApiException.unauthorized("Invalid refresh token.");
            }
            User user = userRepository.findByEmailIgnoreCase(claims.getSubject())
                    .orElseThrow(() -> ApiException.unauthorized("User not found."));
            return tokens(user);
        } catch (JwtException ex) {
            throw ApiException.unauthorized("Invalid or expired refresh token.");
        }
    }

    private AuthResponse tokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getExpirationMs(),
                mapper.toUser(user)
        );
    }
}
