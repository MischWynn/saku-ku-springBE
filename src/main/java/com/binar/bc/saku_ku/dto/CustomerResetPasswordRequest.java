package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerResetPasswordRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Code cannot be empty")
    private String code;

    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
}
