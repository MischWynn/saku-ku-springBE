package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.entity.OtpEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final long OTP_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    // purpose: "REGISTER_VERIFY" | "PASSWORD_RESET" — dipanggil dari CustomerAuthService
    @Transactional
    public void generateAndSend(String email, String purpose) {
        String code = generateCode();

        OtpEntity otp = new OtpEntity();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        otpRepository.save(otp);

        emailService.sendOtpEmail(email, code, purpose);
    }

    // Cocokin email+purpose+code persis, belum dipakai (used=false), belum lewat expiresAt.
    // Begitu ketemu, langsung ditandain used DALAM transaction yang sama biar gak bisa dipakai dua kali
    // (termasuk kalau ada 2 request verify nyaris bareng buat kode yang sama).
    @Transactional
    public void verify(String email, String purpose, String code) {
        OtpEntity otp = otpRepository
                .findByEmailAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(email, purpose, code, LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException("Kode OTP salah atau sudah kadaluarsa"));

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    private String generateCode() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
