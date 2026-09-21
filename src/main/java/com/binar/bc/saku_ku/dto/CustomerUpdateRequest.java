package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerUpdateRequest {

    private String namaLengkap;

    @Pattern(regexp = "\\d{16}", message = "NIK must be 16 digits")
    private String nik;

    @Email(message = "Invalid email format")
    private String email;

    private String noHp;
    private String alamat;
    private LocalDate tanggalLahir;
    private String tipePekerjaan;
    private String pekerjaan;
    private Integer lamaBekerjaBulan;
    private BigDecimal pendapatanBulanan;
    private BigDecimal utangBerjalan;
    private String fotoKtp;

    private String provinsi;
    private String kota;
    private String kecamatan;

    private String namaBank;
    private String nomorRekening;
    private String namaPemilikRekening;
}
