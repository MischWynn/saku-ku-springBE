package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_pengajuan")
@Getter
@Setter
public class PengajuanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_pengajuan")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer", referencedColumnName = "id_customer", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bunga_tenor", referencedColumnName = "id_bunga_tenor", nullable = false)
    private BungaTenorEntity bungaTenor;

    @Column(name = "nominal_pengajuan", nullable = false, precision = 18, scale = 2)
    private BigDecimal nominalPengajuan;

    // Snapshot tenor & interest_rate saat pengajuan dibuat
    @Column(name = "tenor", nullable = false)
    private Integer tenor;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "nominal_disetujui", precision = 18, scale = 2)
    private BigDecimal nominalDisetujui;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // MODAL_USAHA, KONSUMTIF, PENDIDIKAN, KESEHATAN, RENOVASI, LAINNYA — nullable, klien lama/data lama belum ngirim ini
    @Column(name = "tujuan_pinjaman", length = 30)
    private String tujuanPinjaman;

    @Column(name = "tanggal_pengajuan", insertable = false, updatable = false)
    private LocalDateTime tanggalPengajuan;

    // Nullable - diisi PengajuanService.disburse() pas status pindah ke DISBURSED. Data lama
    // (pengajuan yang udah DISBURSED sebelum kolom ini ada) tetap null - dipakai Android buat
    // ngitung jatuh tempo tagihan (fitur Bayar/Tagihan, informational-only).
    @Column(name = "tanggal_pencairan")
    private LocalDateTime tanggalPencairan;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}