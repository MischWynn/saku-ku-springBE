package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerRegisterRequest {

    @NotBlank(message = "Full name cannot be empty")
    private String namaLengkap;

    @Pattern(regexp = "\\d{16}", message = "NIK must be 16 digits")
    private String nik;

    @NotBlank(message = "Phone Number cannot be empty")
    private String noHp;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    private String alamat;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    private LocalDate tanggalLahir;
    private String tipePekerjaan;
    private String pekerjaan;
    private Integer lamaBekerjaBulan;
    private BigDecimal pendapatanBulanan;
    private BigDecimal utangBerjalan;

    private String provinsi;
    private String kota;
    private String kecamatan;

    private String namaBank;
    private String nomorRekening;
    private String namaPemilikRekening;
}