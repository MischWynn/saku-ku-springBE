package com.binar.bc.saku_ku.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.LoginRequestDTO;
import com.binar.bc.saku_ku.service.AuthService;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginKaryawan(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return authService.login(loginRequestDTO);
    }
}
