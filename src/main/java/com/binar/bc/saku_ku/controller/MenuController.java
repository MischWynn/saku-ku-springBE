package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.MenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<List<MenuEntity>> getAllMenus() {
        return ApiResponse.success(menuService.getAllMenus(), "Menus retrieved successfully");
    }

    @PostMapping
    public ApiResponse<MenuEntity> createMenu(@RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.createMenu(request), "Menu created successfully");
    }

    @PatchMapping("/{id}")
    public ApiResponse<MenuEntity> updateMenu(@PathVariable UUID id, @RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.updateMenu(id, request), "Menu updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteMenu(@PathVariable UUID id) {
        menuService.deleteMenu(id);
        return ApiResponse.success(null, "Menu deleted successfully");
    }
}
