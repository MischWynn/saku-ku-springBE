package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.ForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.RegisterRequest;
import com.binar.bc.saku_ku.dto.ResetPasswordRequest;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Auth - Staff", description = "Login staff (SUPERADMIN/MARKETING/BM/BACK_OFFICE), registrasi staff baru (superadmin), lupa/reset password")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Minta token reset password", description = "BELUM ADA integrasi email beneran - token JWT-nya dibalikin langsung di response body (bukan dikirim ke email). Berlaku 15 menit (klaim \"purpose\").")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Username/email tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)))
    })
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        String token = userManagementService.requestForgotPassword(forgotPasswordRequest);
        return ApiResponse.success(token, "Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password pakai token", description = "Mengonsumsi token sekali pakai dari /forgot-password.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password berhasil direset"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token salah/kedaluwarsa", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)))
    })
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        userManagementService.resetPassword(resetPasswordRequest);
        return ApiResponse.success(null, "Password reset successful.");
    }

    @PostMapping
    @Operation(summary = "Registrasi staff baru (superadmin)", description = "Superadmin-only. Field: namaLengkap, username, password, email, roleName.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Staff berhasil dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Field wajib kosong/format salah", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Username/email sudah dipakai", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNPROCESSABLE_EMAIL_TAKEN)))
    })
    public ApiResponse<UserEntity> createUser(@Valid @RequestBody RegisterRequest request) {
    UserEntity user = userManagementService.createUser(request);
    return ApiResponse.success(user, "User created successfully.");
}

}
