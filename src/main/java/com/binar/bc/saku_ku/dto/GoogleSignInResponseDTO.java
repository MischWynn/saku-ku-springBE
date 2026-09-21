package com.binar.bc.saku_ku.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GoogleSignInResponseDTO {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String NEEDS_REGISTRATION = "NEEDS_REGISTRATION";

    private String status;
    private String token;
    private String type;
    private String email;
    private String namaLengkap;

    public static GoogleSignInResponseDTO loggedIn(String token, String email, String namaLengkap) {
        return new GoogleSignInResponseDTO(LOGIN_SUCCESS, token, "Bearer", email, namaLengkap);
    }

    public static GoogleSignInResponseDTO needsRegistration(String email, String namaLengkap) {
        return new GoogleSignInResponseDTO(NEEDS_REGISTRATION, null, null, email, namaLengkap);
    }
}
