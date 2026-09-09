package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.PlafondRequest;
import com.binar.bc.saku_ku.entity.PlafondEntity;
import com.binar.bc.saku_ku.service.PlafondService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plafond")
@RequiredArgsConstructor
public class PlafondController {

    private final PlafondService plafondService;

    @GetMapping
    public ApiResponse<List<PlafondEntity>> getAll() {
        return ApiResponse.success(plafondService.getAll(), "Plafond berhasil diambil");
    }

    @PostMapping
    public ApiResponse<PlafondEntity> create(@RequestBody PlafondRequest request) {
        return ApiResponse.success(plafondService.create(request), "Plafond berhasil dibuat");
    }

    @PatchMapping("/{id}")
    public ApiResponse<PlafondEntity> update(@PathVariable UUID id, @RequestBody PlafondRequest request) {
        return ApiResponse.success(plafondService.update(id, request), "Plafond berhasil diupdate");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable UUID id) {
        plafondService.delete(id);
        return ApiResponse.success(null, "Plafond berhasil dihapus");
    }
}
