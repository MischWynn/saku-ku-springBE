package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.BungaTenorRequest;
import com.binar.bc.saku_ku.entity.BungaTenorEntity;
import com.binar.bc.saku_ku.service.BungaTenorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bunga-tenor")
@RequiredArgsConstructor
public class BungaTenorController {

    private final BungaTenorService bungaTenorService;

    @GetMapping
    public ApiResponse<List<BungaTenorEntity>> getAll() {
        return ApiResponse.success(bungaTenorService.getAll(), "Bunga tenor retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<BungaTenorEntity> getById(@PathVariable UUID id) {
        return ApiResponse.success(bungaTenorService.getById(id), "Bunga tenor retrieved successfully");
    }

    @PostMapping
    public ApiResponse<BungaTenorEntity> create(@Valid @RequestBody BungaTenorRequest request) {
        return ApiResponse.success(bungaTenorService.create(request), "Bunga tenor created successfully");
    }

    @PatchMapping("/{id}")
    public ApiResponse<BungaTenorEntity> update(@PathVariable UUID id, @RequestBody BungaTenorRequest request) {
        return ApiResponse.success(bungaTenorService.update(id, request), "Bunga tenor updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable UUID id) {
        bungaTenorService.delete(id);
        return ApiResponse.success(null, "Bunga tenor deleted successfully");
    }
}