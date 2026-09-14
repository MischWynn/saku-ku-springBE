package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.PengajuanHistoryCustomerDTO;
import com.binar.bc.saku_ku.dto.PengajuanRequest;
import com.binar.bc.saku_ku.dto.PengajuanReviewRequest;
import com.binar.bc.saku_ku.entity.AppCustomerEntity;
import com.binar.bc.saku_ku.entity.PengajuanEntity;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.exception.BusinessRuleException;
import com.binar.bc.saku_ku.service.PengajuanService;
import com.binar.bc.saku_ku.service.ReviewLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pengajuan")
@RequiredArgsConstructor
public class PengajuanController {

    private final PengajuanService pengajuanService;
    private final ReviewLogService reviewLogService;

    // === Customer ===

    @PostMapping
    public ApiResponse<PengajuanEntity> create(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody PengajuanRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.create(currentCustomer.getId(), request);
        return ApiResponse.success(pengajuan, "Pengajuan berhasil dibuat");
    }

    @GetMapping("/me")
    public ApiResponse<List<PengajuanEntity>> getMyPengajuan(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<PengajuanEntity> list = pengajuanService.getByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Riwayat pengajuan berhasil diambil");
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<PengajuanEntity> cancelByCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        PengajuanEntity pengajuan = pengajuanService.cancelByCustomer(id, currentCustomer.getId());
        return ApiResponse.success(pengajuan, "Pengajuan berhasil dibatalkan");
    }

    // Versi customer dari /{id}/history di bawah - endpoint TERPISAH (bukan dibuka bareng),
    // balikin DTO yang difilter (gak ada identitas staff/user internal) + wajib cek
    // kepemilikan dulu, beda dari versi staff yang tetap balikin ReviewLogEntity mentah.
    @GetMapping("/{id}/history/me")
    public ApiResponse<List<PengajuanHistoryCustomerDTO>> getMyHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        PengajuanEntity pengajuan = pengajuanService.getById(id);
        if (!pengajuan.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new BusinessRuleException("Anda tidak berhak mengakses riwayat pengajuan ini");
        }
        List<PengajuanHistoryCustomerDTO> history = reviewLogService.getHistoryByPengajuan(id).stream()
                .map(PengajuanHistoryCustomerDTO::from)
                .toList();
        return ApiResponse.success(history, "Riwayat pengajuan berhasil diambil");
    }

    // === Staff (read) ===

    @GetMapping
    public ApiResponse<List<PengajuanEntity>> getAll() {
        return ApiResponse.success(pengajuanService.getAll(), "Semua pengajuan berhasil diambil");
    }

    @GetMapping("/{id}")
    public ApiResponse<PengajuanEntity> getById(@PathVariable UUID id) {
        return ApiResponse.success(pengajuanService.getById(id), "Pengajuan berhasil diambil");
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<PengajuanEntity>> getByStatus(@PathVariable String status) {
        return ApiResponse.success(pengajuanService.getByStatus(status), "Pengajuan berhasil difilter");
    }

    // === MARKETING actions ===

    @PatchMapping("/{id}/marketing-approve")
    public ApiResponse<PengajuanEntity> marketingApprove(
        @PathVariable UUID id,
        Authentication authentication,
        @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.marketingApprove(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan disetujui Marketing");
    }

    @PatchMapping("/{id}/marketing-reject")
    public ApiResponse<PengajuanEntity> marketingReject(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.marketingReject(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan ditolak Marketing");
    }

    // === BM actions ===

    @PatchMapping("/{id}/bm-approve")
    public ApiResponse<PengajuanEntity> bmApprove(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.bmApprove(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan disetujui BM");
    }

    @PatchMapping("/{id}/bm-reject")
    public ApiResponse<PengajuanEntity> bmReject(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.bmReject(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan ditolak BM");
    }

    // === BACK_OFFICE action ===

    @PatchMapping("/{id}/disburse")
    public ApiResponse<PengajuanEntity> disburse(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        PengajuanEntity pengajuan = pengajuanService.disburse(id, authentication.getName());
        return ApiResponse.success(pengajuan, "Pengajuan berhasil dicairkan");
    }

    // === SUPERADMIN override ===

    @PatchMapping("/{id}/cancel-admin")
    public ApiResponse<PengajuanEntity> cancelBySuperadmin(@PathVariable UUID id) {
        return ApiResponse.success(pengajuanService.cancelBySuperadmin(id), "Pengajuan dibatalkan oleh admin");
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<ReviewLogEntity>> getHistory(@PathVariable UUID id) {
        List<ReviewLogEntity> history = reviewLogService.getHistoryByPengajuan(id);
        return ApiResponse.success(history, "History pengajuan berhasil diambil");
    }
}