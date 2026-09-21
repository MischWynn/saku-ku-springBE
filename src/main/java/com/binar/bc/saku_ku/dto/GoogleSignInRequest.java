package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleSignInRequest {

    @NotBlank(message = "idToken tidak boleh kosong")
    private String idToken;
}
