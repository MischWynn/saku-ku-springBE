package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.PlafondRequest;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.service.PlafondService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plafond")
@RequiredArgsConstructor
@Tag(name = "Master Plafond", description = "Katalog tier plafond (Bronze/Silver/Gold/Platinum) dipakai formula assign plafond customer. GET publik (browsing tanpa login), write superadmin-only.")
public class PlafondController {

    private final PlafondService plafondService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Semua tier plafond (publik)", description = "Bisa diakses tanpa login - dipakai landing page/Android buat nampilin promo tier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    public ApiResponse<List<PlafondEntity>> getAll() {
        return ApiResponse.success(plafondService.getAll(), "Plafond berhasil diambil");
    }

    @PostMapping
    @Operation(summary = "Buat tier plafond baru (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plafond berhasil dibuat"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Field wajib kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<PlafondEntity> create(@RequestBody PlafondRequest request) {
        return ApiResponse.success(plafondService.create(request), "Plafond berhasil dibuat");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update tier plafond (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plafond berhasil diupdate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<PlafondEntity> update(@PathVariable UUID id, @RequestBody PlafondRequest request) {
        return ApiResponse.success(plafondService.update(id, request), "Plafond berhasil diupdate");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus tier plafond (superadmin)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plafond berhasil dihapus"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<String> delete(@PathVariable UUID id) {
        plafondService.delete(id);
        return ApiResponse.success(null, "Plafond berhasil dihapus");
    }
}
