package com.binar.bc.saku_ku.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {
        // secret must be long enough for HS256 (>= 32 bytes)
        service = new JwtService("sakuku-test-secret-key-please-be-long-enough-1234", 60L);
    }

    @Test
    void issueAndParse_roundTrips_usernameAndRole() {
        String token = service.issue("dewi.marketing", "MARKETING", Instant.now());

        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo("dewi.marketing");
        assertThat(claims.get("role")).isEqualTo("MARKETING");
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void issueWithoutExpiry_hasNoExpirationClaim() {
        String token = service.issueWithoutExpiry("dewi.marketing", "MARKETING", Instant.now());

        Claims claims = service.parse(token);

        assertThat(claims.getExpiration()).isNull();
    }

    @Test
    void issueResetToken_andParseResetToken_roundTrips() {
        String token = service.issueResetToken("staff@mail.com", Instant.now());

        Claims claims = service.parseResetToken(token);

        assertThat(claims.getSubject()).isEqualTo("staff@mail.com");
        assertThat(claims.get("purpose")).isEqualTo("reset_password");
    }

    @Test
    void parseResetToken_throws_whenTokenIsNotAResetToken() {
        String normalToken = service.issue("dewi.marketing", "MARKETING", Instant.now());

        assertThatThrownBy(() -> service.parseResetToken(normalToken)).isInstanceOf(JwtException.class);
    }
}
