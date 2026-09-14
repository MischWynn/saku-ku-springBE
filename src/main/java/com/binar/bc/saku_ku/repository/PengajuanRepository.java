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

    // "Ketahan" = udah dianggap makan jatah plafond meski belum cair — pengajuan yang masih
    // di pipeline review (MARKETING_REVIEW/BM_REVIEW/BACKOFFICE_REVIEW) ATAU udah DISBURSED.
    // REJECTED/CANCELLED sengaja gak dihitung (gak jadi ganggu plafond lagi). Pakai
    // nominalDisetujui kalau udah ada (BM udah approve, bisa lebih kecil dari nominalPengajuan),
    // fallback ke nominalPengajuan kalau belum sampai tahap itu.
    @Query("SELECT COALESCE(SUM(COALESCE(p.nominalDisetujui, p.nominalPengajuan)), 0) " +
            "FROM PengajuanEntity p WHERE p.customer.id = :customerId " +
            "AND p.status IN ('MARKETING_REVIEW','BM_REVIEW','BACKOFFICE_REVIEW','DISBURSED')")
    BigDecimal sumHeldNominalByCustomer(@Param("customerId") UUID customerId);

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