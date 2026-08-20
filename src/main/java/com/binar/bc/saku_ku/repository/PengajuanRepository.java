package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.PengajuanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PengajuanRepository extends JpaRepository<PengajuanEntity, UUID> {

    List<PengajuanEntity> findByCustomerId(UUID customerId);

    List<PengajuanEntity> findByStatus(String status);
}