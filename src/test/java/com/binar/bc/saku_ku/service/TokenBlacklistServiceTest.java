package com.binar.bc.saku_ku.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private JwtService jwtService;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private Claims claims;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(redisTemplate, jwtService);
        ReflectionTestUtils.setField(service, "keyPrefix", "sakuku");
    }

    @Test
    void blacklistFromHeader_doesNothing_whenHeaderNull() {
        service.blacklistFromHeader(null);

        verify(jwtService, never()).parse(anyString());
    }

    @Test
    void blacklistFromHeader_doesNothing_whenHeaderMissingBearerPrefix() {
        service.blacklistFromHeader("NotBearer abc");

        verify(jwtService, never()).parse(anyString());
    }

    @Test
    void blacklistFromHeader_storesRemainingTtl_whenTokenNotYetExpired() {
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plus(Duration.ofMinutes(10))));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.blacklistFromHeader("Bearer valid-token");

        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("sakuku:blacklist:valid-token"),
                org.mockito.ArgumentMatchers.eq("1"), any(Duration.class));
    }

    @Test
    void blacklistFromHeader_skipsStorage_whenTokenAlreadyExpired() {
        when(jwtService.parse("expired-token")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().minus(Duration.ofMinutes(1))));

        service.blacklistFromHeader("Bearer expired-token");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isBlacklisted_returnsTrue_whenKeyExists() {
        when(redisTemplate.hasKey("sakuku:blacklist:tok")).thenReturn(true);

        assertThat(service.isBlacklisted("tok")).isTrue();
    }

    @Test
    void isBlacklisted_returnsFalse_whenKeyMissing() {
        when(redisTemplate.hasKey("sakuku:blacklist:tok")).thenReturn(false);

        assertThat(service.isBlacklisted("tok")).isFalse();
    }
}
