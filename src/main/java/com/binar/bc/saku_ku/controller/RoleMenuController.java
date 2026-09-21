package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleMenuRequest;
import com.binar.bc.saku_ku.entity.RoleMenuEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.RoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/role-menu")
@RequiredArgsConstructor
@Tag(name = "Master Access", description = "Matrix permission role x menu (canView/canCreate/canUpdate/canDelete). Dipakai sidebar dinamis + route guard di frontend.")
public class RoleMenuController {

    private final RoleMenuService roleMenuService;
    private final UserRepository userRepository;

    // Dipakai sidebar — akses menu buat role user yang lagi login, resolve dari JWT
    // (bukan dari path {roleId}), jadi semua role staff boleh manggil ini, bukan cuma superadmin.
    @GetMapping("/me")
    @Operation(summary = "Akses menu buat role sendiri", description = "Resolve role dari JWT (bukan path {roleId}) - dipakai sidebar & route guard, semua role staff boleh akses.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<List<RoleMenuEntity>> getMyAccess() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));
        List<RoleMenuEntity> access = roleMenuService.getByRole(user.getRole().getId());
        return ApiResponse.success(access, "OK");
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "Akses menu buat role tertentu (superadmin)", description = "Dipakai halaman Master Access - pilih role, tampilin checkbox permission-nya.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<RoleMenuEntity>> getByRole(@PathVariable UUID roleId) {
        return ApiResponse.success(roleMenuService.getByRole(roleId), "Role access retrieved successfully");
    }

    // Bulk save — 1 request buat semua checkbox yang diubah di halaman Master Access
    @PutMapping
    @Operation(summary = "Simpan matrix akses (bulk, superadmin)", description = "1 request buat semua baris permission 1 role sekaligus (upsert per role+menu).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role access berhasil disimpan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<RoleMenuEntity>> saveRoleMenus(@RequestBody List<RoleMenuRequest> requests) {
        return ApiResponse.success(roleMenuService.upsertBulk(requests), "Role access berhasil disimpan");
    }
}
