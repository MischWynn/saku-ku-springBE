package com.binar.bc.saku_ku.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerLoginRequest {

    @NotBlank(message = "Email/No HP cannot be empty")
    @JsonAlias({"email", "noHp"})
    private String identifier;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}