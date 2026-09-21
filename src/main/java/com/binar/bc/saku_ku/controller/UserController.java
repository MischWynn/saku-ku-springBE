package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User - Staff", description = "Profil staff sendiri (/me), ganti password, dan manajemen akun staff (CRUD) oleh superadmin")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;
    private final TokenBlacklistService tokenBlacklistService;

    // POST /api/v1/user/logout - blacklist token JWT yang lagi dipakai di Redis, sisa umurnya
    // (bukan cuma clear localStorage di browser) - lihat TokenBlacklistService buat alasannya.
    @PostMapping("/logout")
    @Operation(summary = "Logout staff", description = "Blacklist token JWT yang lagi dipakai di Redis (sisa umurnya), bukan cuma clear token lokal.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout berhasil"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token tidak ada/tidak valid", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<String> logout(HttpServletRequest request) {
        tokenBlacklistService.blacklistFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ApiResponse.success(null, "Logout berhasil");
    }

    //PATCH UPDATE /api/v1/user/me
    @PatchMapping("/me")
    @Operation(summary = "Update profil sendiri (partial update)", description = "Field null = gak diubah. Bisa dipanggil role staff manapun (SUPERADMIN/MARKETING/BM/BACK_OFFICE) buat profil sendiri.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profil berhasil diperbarui"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
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
    @Operation(summary = "Ganti password (sudah login)", description = "Beda dari forgot/reset-password (OTP/token-based) - ini verifikasi password LAMA dulu, pola sesi-terautentikasi standar.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password berhasil diganti"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Password lama salah, atau field kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userManagementService.changePassword(username, request);
        return ApiResponse.success(null, "Password berhasil diganti");
    }


    //PATCH /api/v1/user/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Update staff lain (superadmin)", description = "Partial update - namaLengkap/email/status/roleName. Superadmin-only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User berhasil diperbarui"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
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
    @Operation(summary = "Hapus staff (soft-delete, superadmin)", description = "Set deletedDate, bukan hard delete. Superadmin-only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User berhasil dihapus"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<String> deleteUserById(@PathVariable("id") UUID id)
    {
        userService.deleteUserById(id);
        return ApiResponse.success(null, "User deleted successfully");
    }

    //GET /api/v1/user/me

    @GetMapping("/me")
    @Operation(summary = "Profil staff yang sedang login", description = "Ambil namaLengkap + roleName dari JWT subject, dipakai navbar.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
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
    @Operation(summary = "Detail 1 staff (superadmin)", description = "Superadmin-only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<UserEntity> getUserById(@PathVariable UUID id) {
        UserEntity user = userService.getUserById(id);
        return ApiResponse.success(user, "User retrieved successfully");

    }
    //Get All Users
    @GetMapping
    @Operation(summary = "Semua staff (superadmin)", description = "Termasuk yang soft-deleted (deletedDate terisi) - FE Master Staff yang filter row-nya.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        return ApiResponse.success(users, "Users retrieved successfully");
    }
}
