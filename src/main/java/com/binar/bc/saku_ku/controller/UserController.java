package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.ChangePasswordRequest;
import com.binar.bc.saku_ku.dto.UpdateProfileRequest;
import com.binar.bc.saku_ku.dto.UpdateUserRequest;
import com.binar.bc.saku_ku.dto.UserProfileDTO;
import com.binar.bc.saku_ku.entity.AppUserEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.service.TokenBlacklistService;
import com.binar.bc.saku_ku.service.UserManagementService;
import com.binar.bc.saku_ku.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@RestController
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;
    private final TokenBlacklistService tokenBlacklistService;

    // POST /api/v1/user/logout - blacklist token JWT yang lagi dipakai di Redis, sisa umurnya
    // (bukan cuma clear localStorage di browser) - lihat TokenBlacklistService buat alasannya.
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest request) {
        tokenBlacklistService.blacklistFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ApiResponse.success(null, "Logout berhasil");
    }

    //PATCH UPDATE /api/v1/user/me
    @PatchMapping("/me")
    public ApiResponse<UserEntity> updateOwnProfile(
        @AuthenticationPrincipal AppUserEntity currentUser,
        @RequestBody UpdateProfileRequest request
) {
        UserEntity user = userService.updateOwnProfile(
                currentUser.getUsername(), request.getNamaLengkap(), request.getEmail());
        return ApiResponse.success(user, "Profile updated successfully");
    }

    //PATCH /api/v1/user/change-password — ganti password saat udah login, beda dari forgot/reset-password
    @PatchMapping("/change-password")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.changePassword(username, request);
        return ApiResponse.success(null, "Password berhasil diganti");
    }


    //PATCH /api/v1/user/{id}
    @PatchMapping("/{id}")
    public ApiResponse<UserEntity> updateUserBySuperadmin(
        @PathVariable UUID id, 
        @RequestBody UpdateUserRequest request
    ) {
        UserEntity user = userService.updateUserBySuperadmin(
                id, request.getNamaLengkap(), request.getEmail(), request.getStatus(), request.getRoleName());
        return ApiResponse.success(user, "User updated successfully");
    }
    
    //DELETE /api/v1/user/{id}
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUserById(@PathVariable("id") UUID id)
    {
        userService.deleteUserById(id);
        return ApiResponse.success(null, "User deleted successfully");
    }

    //GET /api/v1/user/me

    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));

        UserProfileDTO dto = new UserProfileDTO(
            user.getNamaLengkap(),
            user.getRole().getNamaRole()
        );
        return ApiResponse.success(dto, "OK");
    }

    // @GetMapping("/me")
    // public ApiResponse<UserEntity> getCurrentUser(@AuthenticationPrincipal AppUserEntity currentUser) {
    //     UserEntity user = userService.getUserByUsername(currentUser.getUsername());
    //     return ApiResponse.success(user, "Current user retrieved successfully");
    // }

    //GET /api/v1/user/{id}
    @GetMapping("/{id}")
    public ApiResponse<UserEntity> getUserById(@PathVariable UUID id) {
        UserEntity user = userService.getUserById(id);
        return ApiResponse.success(user, "User retrieved successfully");
        
    }
    //Get All Users
    @GetMapping
    public ApiResponse<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        return ApiResponse.success(users, "Users retrieved successfully");
    }
}
