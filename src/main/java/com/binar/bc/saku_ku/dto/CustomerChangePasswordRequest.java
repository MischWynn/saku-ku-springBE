package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerChangePasswordRequest {

    @NotBlank(message = "Password lama wajib diisi")
    private String oldPassword;

    @NotBlank(message = "Password baru wajib diisi")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "Password minimal 8 karakter dan harus mengandung huruf besar, huruf kecil, dan angka")
    private String newPassword;
}
