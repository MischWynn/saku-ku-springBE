package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// Semua field optional — partial update, pola sama kayak UpdateUserRequest (staff).
// NIK dan password sengaja TIDAK ada di sini: NIK itu identitas permanen, ganti password
// punya jalur sendiri (forgot/reset-password), bukan lewat edit profil biasa.
@Getter
@Setter
public class CustomerUpdateRequest {

    private String namaLengkap;

    @Email(message = "Invalid email format")
    private String email;

    private String noHp;
    private String alamat;
    private LocalDate tanggalLahir;
    // ASN_TNI_POLRI, BUMN_BUMD, SWASTA, WIRASWASTA, NON_PROFIT, FREELANCE, TIDAK_BEKERJA
    private String tipePekerjaan;
    private String pekerjaan;
    private Integer lamaBekerjaBulan;
    private BigDecimal pendapatanBulanan;
    private BigDecimal utangBerjalan;
}
