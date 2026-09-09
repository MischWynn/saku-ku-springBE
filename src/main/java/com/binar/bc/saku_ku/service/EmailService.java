package com.binar.bc.saku_ku.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String code, String purpose) {
        String subject = "REGISTER_VERIFY".equals(purpose)
                ? "Kode Verifikasi Akun Saku-ku"
                : "Kode Reset Password Saku-ku";

        String body = "Kode OTP Anda: " + code + "\n\n"
                + "Kode ini berlaku selama 10 menit. Jangan bagikan kode ini ke siapa pun.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
