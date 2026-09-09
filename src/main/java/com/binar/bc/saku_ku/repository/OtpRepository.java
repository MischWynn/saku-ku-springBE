package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<OtpEntity, UUID> {
    Optional<OtpEntity> findByEmailAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(
            String email, String purpose, String code, LocalDateTime now);
}
