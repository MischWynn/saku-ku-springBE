package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDeleteAccountRequest {

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
