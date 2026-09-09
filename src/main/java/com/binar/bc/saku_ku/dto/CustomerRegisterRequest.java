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

    @NotBlank(message = "NIK cannot be empty")
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

    // Opsional dulu (Android customer app belum ada yang ngirim ini)
    private LocalDate tanggalLahir;
    private String tipePekerjaan; // KARYAWAN, WIRASWASTA, LAINNYA
    private String pekerjaan;
    private Integer lamaBekerjaBulan;
    private BigDecimal pendapatanBulanan;
    private BigDecimal utangBerjalan;
}