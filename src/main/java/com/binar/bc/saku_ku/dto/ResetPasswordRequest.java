package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {

    @NotBlank(message = "Email can't be empty")
    private String email;

    private String token;

    @NotBlank(message = "New Password can't be empty")
    @Size(min = 8, message = "New Password must have at least 8 characters")
    private String newPassword;
}
