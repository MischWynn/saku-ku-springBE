package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.RoleRequest;
import com.binar.bc.saku_ku.entity.RoleEntity;
import com.binar.bc.saku_ku.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleEntity>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles(), "Roles retrieved successfully");
    }

    @PostMapping
    public ApiResponse<RoleEntity> createRole(@RequestBody RoleRequest request) {
        return ApiResponse.success(roleService.createRole(request), "Role created successfully");
    }

    @PatchMapping("/{id}")
    public ApiResponse<RoleEntity> updateRole(@PathVariable UUID id, @RequestBody RoleRequest request) {
        RoleEntity role = roleService.updateRole(request.getNama(), request.getDescription(), id);
        return ApiResponse.success(role, "Role updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRole(@PathVariable UUID id) {
        roleService.deleteRoleById(id);
        return ApiResponse.success(null, "Role deleted successfully");
    }
}
