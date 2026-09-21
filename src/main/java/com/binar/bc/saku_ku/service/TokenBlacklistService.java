package com.binar.bc.saku_ku.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;


// ini penggunaan redis untuk blacklist token JWT yang sudah logout, biar token itu gak bisa dipakai lagi walau umurnya belum habis. Dipanggil dari endpoint /logout (staff & customer) dan dicek di JwtAuthFilter tiap request yang bawa token.
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "Bearer ";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    @Value("${app.redis.key-prefix}")
    private String keyPrefix;

    // Dipanggil dari endpoint /logout (staff & customer) - header Authorization mentah, bukan token yang udah di-strip, biar controller gak perlu tau soal "Bearer " prefix sendiri.
    public void blacklistFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return;
        }
        String token = authorizationHeader.substring(PREFIX.length());

        Claims claims = jwtService.parse(token);
        Instant expiresAt = claims.getExpiration().toInstant();
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }

        // TTL entry Redis disamain persis sisa umur token - begitu tokennya toh udah kadaluarsa sendiri, entry blacklist-nya ikut ilang otomatis, gak numpuk selamanya di Redis.
        redisTemplate.opsForValue().set(key(token), "1", remaining);
    }

    // Dicek di JwtAuthFilter, tiap request yang bawa token - sebelum token itu diterima jadi authentication yang valid.
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
    }

    private String key(String token) {
        return keyPrefix + ":blacklist:" + token;
    }
}
