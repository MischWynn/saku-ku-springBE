package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.ForgotPasswordRequest;
import com.binar.bc.saku_ku.dto.RegisterRequest;
import com.binar.bc.saku_ku.dto.ResetPasswordRequest;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor

public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        String token = userManagementService.requestForgotPassword(forgotPasswordRequest);
        return ApiResponse.success(token, "Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        userManagementService.resetPassword(resetPasswordRequest);
        return ApiResponse.success(null, "Password reset successful.");
    }
    @PostMapping
    public ApiResponse<UserEntity> createUser(@Valid @RequestBody RegisterRequest request) {
    UserEntity user = userManagementService.createUser(request);
    return ApiResponse.success(user, "User created successfully.");
}
    
}