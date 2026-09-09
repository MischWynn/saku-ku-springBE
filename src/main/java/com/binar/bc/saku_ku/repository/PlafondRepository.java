package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.PlafondEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlafondRepository extends JpaRepository<PlafondEntity, UUID> {
    List<PlafondEntity> findByStatusOrderByLimitMaksimalAsc(String status);
}
