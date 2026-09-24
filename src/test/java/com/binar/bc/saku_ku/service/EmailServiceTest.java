package com.binar.bc.saku_ku.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
    }

    @Test
    void sendOtpEmail_usesVerificationCopy_forRegisterVerify() {
        service.sendOtpEmail("customer@mail.com", "123456", "REGISTER_VERIFY");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("customer@mail.com");
        assertThat(sent.getSubject()).contains("Verifikasi");
        assertThat(sent.getText()).contains("123456");
    }

    @Test
    void sendOtpEmail_usesResetPasswordCopy_forOtherPurpose() {
        service.sendOtpEmail("customer@mail.com", "654321", "RESET_PASSWORD");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Reset Password");
        assertThat(sent.getText()).contains("654321");
    }
}
