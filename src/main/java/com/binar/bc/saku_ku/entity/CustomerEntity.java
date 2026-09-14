package com.binar.bc.saku_ku.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tbl_customer")
@Getter
@Setter
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_customer")
    private UUID id;

    @Column(name = "nama_lengkap", nullable = false, length = 150)
    private String namaLengkap;

    @Column(name = "nik", nullable = false, unique = true, length = 16)
    private String nik;

    @Column(name = "no_hp", nullable = false, unique = true, length = 20)
    private String noHp;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "alamat", columnDefinition = "TEXT")
    private String alamat;

    @Column(name = "plafond", nullable = false, precision = 18, scale = 2)
    private BigDecimal plafond;

    @ JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Ditambah 3 Sept 2026 (lanjutan migration tujuan_pinjaman) — semua nullable,
    // data lama & customer yang belum lengkapin profil bakal null. Dipakai buat
    // scoring input di drawer (Employment & Financial section, FE).
    @Column(name = "tanggal_lahir")
    private LocalDate tanggalLahir;

    // ASN_TNI_POLRI, BUMN_BUMD, SWASTA, WIRASWASTA, NON_PROFIT, FREELANCE, TIDAK_BEKERJA —
    // plain String, pola sama kayak `status` (bukan @Enumerated). Diperluas dari 4 ke 7
    // kategori 10 Sept 2026; value lama (KARYAWAN/PNS) tetap valid di data existing.
    @Column(name = "tipe_pekerjaan", length = 30)
    private String tipePekerjaan;

    @Column(name = "pekerjaan", length = 150)
    private String pekerjaan;

    @Column(name = "lama_bekerja_bulan")
    private Integer lamaBekerjaBulan;

    @Column(name = "pendapatan_bulanan", precision = 18, scale = 2)
    private BigDecimal pendapatanBulanan;

    @Column(name = "utang_berjalan", precision = 18, scale = 2)
    private BigDecimal utangBerjalan;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}