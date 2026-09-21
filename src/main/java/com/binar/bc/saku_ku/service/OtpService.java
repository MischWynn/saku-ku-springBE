package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

// ini akhirnya pakai redis, OTPnya biar ephemeral (mati sendiri abis 10 menit) dan single-use (DEL abis dipakai) - gak ada lagi OtpEntity/OtpRepository lama yang nyimpen OTP di Postgres.
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.redis.key-prefix}")
    private String keyPrefix;

    // purpose: "REGISTER_VERIFY" | "PASSWORD_RESET" — dipanggil dari CustomerAuthService
    public void generateAndSend(String email, String purpose) {
        String code = generateCode();
        redisTemplate.opsForValue().set(key(purpose, email), code, OTP_TTL);
        emailService.sendOtpEmail(email, code, purpose);
    }

    // Konsumsi kode (DEL abis cocok) - dipakai pas reset-password/verify-otp BENERAN disubmit, jadi kode yang sama gak bisa dipakai dua kali.
    public void verify(String email, String purpose, String code) {
        String key = key(purpose, email);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(code)) {
            throw new BusinessRuleException("Kode OTP salah atau sudah kadaluarsa");
        }
        redisTemplate.delete(key);
    }

    // Cek validitas doang, TIDAK di-DEL - dipakai di alur reset-password: layar Verifikasi perlu mastiin kode bener SEBELUM lanjut ke layar Ganti Password, tapi konsumsi kode yang sebenarnya tetap kejadian sekali di verify() pas reset-password beneran di-submit.
    public boolean isValid(String email, String purpose, String code) {
        String stored = redisTemplate.opsForValue().get(key(purpose, email));
        return stored != null && stored.equals(code);
    }

    private String key(String purpose, String email) {
        return keyPrefix + ":otp:" + purpose + ":" + email;
    }

    private String generateCode() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
