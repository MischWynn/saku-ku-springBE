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

    @Column(name = "nik", unique = true, length = 16)
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

    @Column(name = "tanggal_lahir")
    private LocalDate tanggalLahir;

    // ASN_TNI_POLRI, BUMN_BUMD, SWASTA, WIRASWASTA, NON_PROFIT, FREELANCE, TIDAK_BEKERJA —
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

    // Foto KTP - Base64, nullable. Disengaja TEXT bukan file-storage/multipart beneran
    @JsonIgnore
    @Column(name = "foto_ktp", columnDefinition = "TEXT")
    private String fotoKtp;

    // Domisili sebagai cascading dropdown (Provinsi -> Kota -> Kecamatan), disimpen nama-string langsung
    @Column(name = "provinsi", length = 100)
    private String provinsi;

    @Column(name = "kota", length = 100)
    private String kota;

    @Column(name = "kecamatan", length = 100)
    private String kecamatan;

    @Column(name = "nama_bank", length = 100)
    private String namaBank;

    @Column(name = "nomor_rekening", length = 50)
    private String nomorRekening;

    @Column(name = "nama_pemilik_rekening", length = 150)
    private String namaPemilikRekening;

    // Token registrasi FCM (Firebase Cloud Messaging), dikirim Android abis login/dapet token baru dari sistem.
    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    // Soft-delete untuk customer.
    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}