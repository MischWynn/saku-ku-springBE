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
        String subject;
        String body;

        // Pengecekan kondisi berdasarkan 'purpose' untuk membedakan isi email
        if ("REGISTER_VERIFY".equals(purpose)) {
            subject = "Kode Verifikasi Akun Saku-ku";
            body = """
                    Hallo, %s
                    
                    Verifikasi Akun
                    Sebelum memulai perjalanan dengan Saku-ku, konfirmasi akunmu terlebih dahulu dengan memasukan kode OTP berikut ini untuk menyelesaikan proses verifikasi aplikasi Saku-ku.
                    
                    %s
                    
                    Kode OTP hanya berlaku 10 menit dan bersifat rahasia. Mohon untuk tidak membagikan kode ini kepada siapapun termasuk pihak yang mengatasnamakan PT Saku-ku.
                    
                    Email ini dibuat otomatis, mohon untuk tidak membalas, jika ada pertanyaan atau membutuhkan bantuan silakan hubungi call center kami di 021-123 atau melalui email di cs@ksaku-ku.id
                    """.formatted(to, code);
                    
        } else {
            subject = "Kode Reset Password Saku-ku";
            body = """
                    Hallo, %s
                    
                    Permintaan Reset Password
                    Kami menerima permintaan untuk melakukan reset password pada akun kamu. Silakan masukkan kode OTP berikut ini untuk membuat password baru.
                    
                    %s
                    
                    Kode OTP ini hanya berlaku 10 menit. Jika kamu tidak pernah merasa meminta reset password, abaikan email ini dan pastikan akun kamu tetap aman.
                    
                    Email ini dibuat otomatis, mohon untuk tidak membalas, jika ada pertanyaan atau membutuhkan bantuan silakan hubungi call center kami di 021-123 atau melalui email di cs@ksaku-ku.id
                    """.formatted(to, code);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        

        mailSender.send(message);
    }
}