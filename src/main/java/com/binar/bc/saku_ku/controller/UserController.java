package com.binar.bc.saku_ku.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.UpdateProfileRequest;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ApiResponse<UserEntity> updateOwnProfile(
        @AuthenticationPrincipal AppUserEntity currentUser,
        @RequestBody UpdateProfileRequest request
) {
    String currentUsername = currentUser.getUsername();
    UserEntity user = userService.updateOwnProfile(currentUsername, request.getNamaLengkap(), request.getEmail());
    return ApiResponse.success(user, "Profile updated successfully");
}
}
