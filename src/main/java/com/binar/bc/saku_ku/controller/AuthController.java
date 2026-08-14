package com.binar.bc.saku_ku.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;
    private final AppUserService appUserService;
    private final UserService userService;


    @PostMapping("/register")
        public ApiResponse<UserEntity> register(@RequestBody RegisterRequest request) {
            return authService.register(request);
        }
    )

    @PostMapping("/forgot-password")
    public ApiResponse<UserEntity> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        String token = authService.requestForgotPassword(forgotPasswordRequest);
        return ApiResponse.success(token, "Forgot password request successful. Please check your email for further instructions.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<UserEntity> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        UserEntity user = authService.resetPassword(resetPasswordRequest);
        return ApiResponse.success(user, "Password reset successful.");
    }
    
}
