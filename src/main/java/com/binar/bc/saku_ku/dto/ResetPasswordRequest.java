package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {

    @NotBlank(message = "Email tidak boleh kosong")
    private String email;

    private String token;

    @NotBlank(message = "Password baru tidak boleh kosong")
    @Size(min = 8, message = "Password baru harus memiliki minimal 8 karakter")
    private String newPassword;
}
