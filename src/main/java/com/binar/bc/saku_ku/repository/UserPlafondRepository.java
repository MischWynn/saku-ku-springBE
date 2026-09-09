package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.UserPlafondEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPlafondRepository extends JpaRepository<UserPlafondEntity, UUID> {
    Optional<UserPlafondEntity> findByCustomer_Id(UUID customerId);
}
