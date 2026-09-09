package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_otp")
@Getter
@Setter
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_otp")
    private UUID id;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    // REGISTER_VERIFY, PASSWORD_RESET — plain String, pola sama kayak CustomerEntity.status/tipePekerjaan
    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
