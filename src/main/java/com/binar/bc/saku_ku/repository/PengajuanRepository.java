package com.binar.bc.saku_ku.repository;

import com.binar.bc.saku_ku.entity.PengajuanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PengajuanRepository extends JpaRepository<PengajuanEntity, UUID> {

    long countByStatus(String status);   // ← dibenerin dari countByCustomer

    @Query("SELECT COALESCE(SUM(p.nominalDisetujui), 0) FROM PengajuanEntity p WHERE p.status = 'DISBURSED'")
    BigDecimal sumNominalDisetujuiDisbursed();

    @Query(value = """
        SELECT DATE(tanggal_pengajuan) AS day, COUNT(*) AS total
        FROM vili.tbl_pengajuan
        WHERE tanggal_pengajuan >= :startDate
        GROUP BY DATE(tanggal_pengajuan)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> countPengajuanPerDaySince(@Param("startDate") LocalDateTime startDate);

    List<PengajuanEntity> findByCustomerId(UUID customerId);

    List<PengajuanEntity> findByStatus(String status);
}