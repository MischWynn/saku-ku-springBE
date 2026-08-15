package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegisterRequest {

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    private String namaLengkap;

    @NotBlank(message = "NIK tidak boleh kosong")
    @Pattern(regexp = "\\d{16}", message = "NIK harus 16 digit angka")
    private String nik;

    @NotBlank(message = "Nomor HP tidak boleh kosong")
    private String noHp;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    private String alamat;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}