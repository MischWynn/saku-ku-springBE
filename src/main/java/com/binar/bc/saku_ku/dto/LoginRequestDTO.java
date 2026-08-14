package com.binar.bc.saku_ku.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    
    @NotBlank(message = "Username is required")
    @JsonAlias({"username", "email"})
    private String username;
    private String password;
}
