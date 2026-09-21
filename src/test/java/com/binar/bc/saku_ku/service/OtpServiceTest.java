package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OtpService (rewritten 19 Sept to use Redis instead of a JPA entity).
 * StringRedisTemplate + its ValueOperations, and EmailService, are fully mocked — no real Redis.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailService emailService;

    private OtpService otpService;

    private static final String KEY_PREFIX = "vili";
    private static final String EMAIL = "customer@example.com";
    private static final String PURPOSE = "REGISTER_VERIFY";

    @BeforeEach
    void setUp() {
        otpService = new OtpService(redisTemplate, emailService);
        ReflectionTestUtils.setField(otpService, "keyPrefix", KEY_PREFIX);
    }

    private String expectedKey() {
        return KEY_PREFIX + ":otp:" + PURPOSE + ":" + EMAIL;
    }

    @Nested
    class GenerateAndSend {

        @Test
        void storesCodeInRedis_withTenMinuteTtl_andSendsEmail() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            otpService.generateAndSend(EMAIL, PURPOSE);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(keyCaptor.capture(), codeCaptor.capture(), eq(Duration.ofMinutes(10)));

            assertThat(keyCaptor.getValue()).isEqualTo(expectedKey());
            String code = codeCaptor.getValue();
            assertThat(code).matches("\\d{6}");

            // The exact same code that was stored must be the one emailed out.
            verify(emailService).sendOtpEmail(EMAIL, code, PURPOSE);
        }
    }

    @Nested
    class Verify {

        @Test
        void succeeds_andDeletesKey_onCorrectCode() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn("123456");

            otpService.verify(EMAIL, PURPOSE, "123456");

            verify(redisTemplate).delete(expectedKey());
        }

        @Test
        void throwsBusinessRuleException_onWrongCode_andDoesNotDelete() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn("123456");

            assertThatThrownBy(() -> otpService.verify(EMAIL, PURPOSE, "000000"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Kode OTP salah atau sudah kadaluarsa");

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        void throwsBusinessRuleException_whenCodeMissingOrExpired() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn(null);

            assertThatThrownBy(() -> otpService.verify(EMAIL, PURPOSE, "123456"))
                    .isInstanceOf(BusinessRuleException.class);

            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    class IsValid {

        @Test
        void returnsTrue_onCorrectCode_withoutConsumingIt() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn("654321");

            boolean valid = otpService.isValid(EMAIL, PURPOSE, "654321");

            assertThat(valid).isTrue();
            // The whole point of isValid() vs verify() is that it never consumes the code.
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        void returnsFalse_onWrongCode_withoutConsumingIt() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn("654321");

            boolean valid = otpService.isValid(EMAIL, PURPOSE, "000000");

            assertThat(valid).isFalse();
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        void returnsFalse_whenNoCodeStored_withoutConsumingIt() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey())).thenReturn(null);

            boolean valid = otpService.isValid(EMAIL, PURPOSE, "123456");

            assertThat(valid).isFalse();
            verify(redisTemplate, never()).delete(anyString());
        }
    }
}
