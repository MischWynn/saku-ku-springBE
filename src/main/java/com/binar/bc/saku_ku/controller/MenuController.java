package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.MenuRequest;
import com.binar.bc.saku_ku.entity.MenuEntity;
import com.binar.bc.saku_ku.service.MenuService;
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
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "Master Menu", description = "CRUD data menu sidebar (nama, path, icon, urutan) - dipakai Master Access buat matrix permission. Superadmin only.")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Semua menu")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<MenuEntity>> getAllMenus() {
        return ApiResponse.success(menuService.getAllMenus(), "Menus retrieved successfully");
    }

    @PostMapping
    @Operation(summary = "Buat menu baru")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menu berhasil dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Field wajib kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<MenuEntity> createMenu(@RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.createMenu(request), "Menu created successfully");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update menu")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menu berhasil diupdate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<MenuEntity> updateMenu(@PathVariable UUID id, @RequestBody MenuRequest request) {
        return ApiResponse.success(menuService.updateMenu(id, request), "Menu updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus menu")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menu berhasil dihapus"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<String> deleteMenu(@PathVariable UUID id) {
        menuService.deleteMenu(id);
        return ApiResponse.success(null, "Menu deleted successfully");
    }
}
