package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.AppUserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-ttl-minutes}") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(AppUserEntity user, Instant issuedAt) {
        return builder(user, issuedAt)
                .expiration(Date.from(issuedAt.plus(ttl)))
                .compact();
    }

    public String issueWithoutExpiry(AppUserEntity user, Instant issuedAt) {
        return builder(user, issuedAt).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtBuilder builder(AppUserEntity user, Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(Date.from(issuedAt))
                .signWith(key);
    }

    // ==== KHUSUS RESET PASSWORD (JWT stateless) ====

    public String issueResetToken(String email, Instant issuedAt) {
        Instant expiry = issuedAt.plus(Duration.ofMinutes(15));
        return Jwts.builder()
                .subject(email)
                .claim("purpose", "reset_password")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parseResetToken(String token) {
        Claims claims = parse(token);

        Object purpose = claims.get("purpose");
        if (!"reset_password".equals(purpose)) {
            throw new JwtException("Token bukan untuk reset password");
        }
        return claims;
    }
}