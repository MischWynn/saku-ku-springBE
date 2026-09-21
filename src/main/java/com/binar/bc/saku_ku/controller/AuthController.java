package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth - Staff", description = "Login staff (SUPERADMIN/MARKETING/BM/BACK_OFFICE)")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // permitAll di SecurityConfig - endpoint ini gak butuh token sama sekali
    @Operation(summary = "Login staff", description = "Balikin JWT mentah {token,type} - BUKAN dibungkus ApiResponse<>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login berhasil", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJTVVBFUkFETUlOIn0.abc123\",\"type\":\"Bearer\"}"))),
            @ApiResponse(responseCode = "400", description = "Username/password kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @ApiResponse(responseCode = "401", description = "Username atau password salah", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_LOGIN)))
    })
    public ResponseEntity<AuthResponseDTO> loginKaryawan(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return authService.login(loginRequestDTO);
    }
}
