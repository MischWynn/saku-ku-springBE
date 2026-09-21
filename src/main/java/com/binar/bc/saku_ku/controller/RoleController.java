package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
@Tag(name = "Master Role", description = "CRUD role staff (SUPERADMIN/MARKETING/BM/BACK_OFFICE) - superadmin only")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Semua role")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<RoleEntity>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles(), "Roles retrieved successfully");
    }

    @PostMapping
    @Operation(summary = "Buat role baru", description = "Body pakai field \"nama\" (bukan \"namaRole\" - beda nama sengaja dari desain DTO request vs entity).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role berhasil dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Field wajib kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<RoleEntity> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.success(roleService.createRole(request), "Role created successfully");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update role")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role berhasil diupdate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<RoleEntity> updateRole(@PathVariable UUID id, @RequestBody RoleRequest request) {
        RoleEntity role = roleService.updateRole(request.getNama(), request.getDescription(), id);
        return ApiResponse.success(role, "Role updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus role (hard delete)", description = "BEDA dari User yang soft-delete - ini beneran hapus baris DB. Gagal (constraint violation) kalau role masih dipakai staff aktif.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role berhasil dihapus"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<String> deleteRole(@PathVariable UUID id) {
        roleService.deleteRoleById(id);
        return ApiResponse.success(null, "Role deleted successfully");
    }
}
