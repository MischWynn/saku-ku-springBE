package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ForgotPasswordRequest {
    @NotBlank(message = "Please provide your email address!")
    @Email(message = "Email format is invalid!")
    private String email;

    
}
