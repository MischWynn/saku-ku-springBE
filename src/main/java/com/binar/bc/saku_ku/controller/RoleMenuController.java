package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleMenuRequest;
import com.binar.bc.saku_ku.entity.RoleMenuEntity;
import com.binar.bc.saku_ku.entity.UserEntity;
import com.binar.bc.saku_ku.exception.UnauthorizedException;
import com.binar.bc.saku_ku.repository.UserRepository;
import com.binar.bc.saku_ku.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/role-menu")
@RequiredArgsConstructor
public class RoleMenuController {

    private final RoleMenuService roleMenuService;
    private final UserRepository userRepository;

    // Dipakai sidebar — akses menu buat role user yang lagi login, resolve dari JWT
    // (bukan dari path {roleId}), jadi semua role staff boleh manggil ini, bukan cuma superadmin.
    @GetMapping("/me")
    public ApiResponse<List<RoleMenuEntity>> getMyAccess() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User tidak ditemukan"));
        List<RoleMenuEntity> access = roleMenuService.getByRole(user.getRole().getId());
        return ApiResponse.success(access, "OK");
    }

    @GetMapping("/role/{roleId}")
    public ApiResponse<List<RoleMenuEntity>> getByRole(@PathVariable UUID roleId) {
        return ApiResponse.success(roleMenuService.getByRole(roleId), "Role access retrieved successfully");
    }

    // Bulk save — 1 request buat semua checkbox yang diubah di halaman Master Access
    @PutMapping
    public ApiResponse<List<RoleMenuEntity>> saveRoleMenus(@RequestBody List<RoleMenuRequest> requests) {
        return ApiResponse.success(roleMenuService.upsertBulk(requests), "Role access berhasil disimpan");
    }
}
