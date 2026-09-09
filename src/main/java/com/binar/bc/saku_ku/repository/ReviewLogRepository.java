package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, UUID> {

    List<ReviewLogEntity> findByPengajuanIdOrderByCreatedAtAsc(UUID pengajuanId);

    List<ReviewLogEntity> findTop10ByOrderByCreatedAtDesc();

    List<ReviewLogEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}