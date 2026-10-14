package com.college.booking.security;

import com.college.booking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = (secret + "0123456789abcdef0123456789abcdef").getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(java.util.Arrays.copyOf(bytes, Math.max(32, bytes.length)));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(User user) {
        return build(user, expirationMs, "access");
    }

    public String generateRefreshToken(User user) {
        return build(user, refreshExpirationMs, "refresh");
    }

    private String build(User user, long ttl, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        "uid", user.getId(),
                        "role", user.getRole().name(),
                        "name", user.getFullName(),
                        "typ", type
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public boolean isAccessToken(String token) {
        Object typ = parse(token).get("typ");
        return typ == null || "access".equals(typ);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
