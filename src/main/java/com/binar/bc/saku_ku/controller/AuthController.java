package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.AuthResponseDTO;
import com.binar.bc.saku_ku.dto.LoginRequestDTO;
import com.binar.bc.saku_ku.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletResponse response
    ) {
        ResponseEntity<AuthResponseDTO> result = authService.login(loginRequestDTO);
        String token = result.getBody().getToken();

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // ganti true kalau nanti deploy pakai HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(60))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return result;
    }
}