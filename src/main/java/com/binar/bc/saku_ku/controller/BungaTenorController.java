package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.BungaTenorRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.service.BungaTenorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bunga-tenor")
@RequiredArgsConstructor
@Tag(name = "Master Rate", description = "Tenor & suku bunga pinjaman. GET publik (simulasi cicilan customer/Android tanpa login), write superadmin-only.")
public class BungaTenorController {

    private final BungaTenorService bungaTenorService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Semua tenor (publik)", description = "Dipakai widget simulasi cicilan & dropdown tenor pengajuan.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    public ApiResponse<List<BungaTenorEntity>> getAll() {
        return ApiResponse.success(bungaTenorService.getAll(), "Bunga tenor retrieved successfully");
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Detail 1 tenor (publik)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<BungaTenorEntity> getById(@PathVariable UUID id) {
        return ApiResponse.success(bungaTenorService.getById(id), "Bunga tenor retrieved successfully");
    }

    @PostMapping
    @Operation(summary = "Buat tenor baru (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenor berhasil dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Field wajib kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<BungaTenorEntity> create(@Valid @RequestBody BungaTenorRequest request) {
        return ApiResponse.success(bungaTenorService.create(request), "Bunga tenor created successfully");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update tenor (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenor berhasil diupdate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<BungaTenorEntity> update(@PathVariable UUID id, @RequestBody BungaTenorRequest request) {
        return ApiResponse.success(bungaTenorService.update(id, request), "Bunga tenor updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus tenor (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenor berhasil dihapus"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<String> delete(@PathVariable UUID id) {
        bungaTenorService.delete(id);
        return ApiResponse.success(null, "Bunga tenor deleted successfully");
    }
}
