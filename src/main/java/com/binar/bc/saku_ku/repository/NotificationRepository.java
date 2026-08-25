package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<NotificationEntity> findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(UUID customerId);

    long countByCustomerIdAndIsReadFalse(UUID customerId);
}
