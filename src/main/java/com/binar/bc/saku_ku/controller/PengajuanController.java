package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Pengajuan", description = "Alur pengajuan pinjaman: create (customer) -> marketing -> BM -> back office -> disburse")
public class PengajuanController {

    private static final String PENGAJUAN_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":null,"status":"MARKETING_REVIEW","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private static final String PENGAJUAN_DISBURSED_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":8000000,"status":"DISBURSED","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":"2026-09-21T14:30:00","createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-21T14:30:00"}""";

    // Varian status lain, ditulis literal - BUKAN PENGAJUAN_EXAMPLE.replace(...), karena
    // annotation value harus compile-time constant dan pemanggilan method (termasuk .replace())
    // gak dianggap konstan meski receiver-nya sendiri konstan.
    private static final String PENGAJUAN_BM_REVIEW_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":null,"status":"BM_REVIEW","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private static final String PENGAJUAN_MARKETING_REJECTED_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":null,"status":"MARKETING_REJECTED","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private static final String PENGAJUAN_BACKOFFICE_REVIEW_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":8000000,"status":"BACKOFFICE_REVIEW","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private static final String PENGAJUAN_BM_REJECTED_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":null,"status":"BM_REJECTED","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private static final String PENGAJUAN_CANCELLED_EXAMPLE = """
            {"id":"7a1b2c3d-4e5f-6789-0abc-def123456789","customer":{"id":"3f5b1a2e-1234-4a3b-9c1d-abcdef123456","namaLengkap":"Novita Sari","email":"novita.sari@mail.com"},"bungaTenor":{"id":"9f8e7d6c-5b4a-3210-fedc-ba9876543210","tenor":12,"interestRate":3.0},"nominalPengajuan":8000000,"tenor":12,"interestRate":3.0,"nominalDisetujui":null,"status":"CANCELLED","tujuanPinjaman":"MODAL_USAHA","tanggalPengajuan":"2026-09-20T09:00:00","tanggalPencairan":null,"createdAt":"2026-09-20T09:00:00","updatedAt":"2026-09-20T09:00:00"}""";

    private final PengajuanService pengajuanService;
    private final ReviewLogService reviewLogService;

    // === Customer ===

    @PostMapping
    @Operation(summary = "Ajukan pinjaman baru", description = "Ditolak dengan 422 kalau nominalPengajuan melebihi sisaPlafond customer (limit total dikurangi pengajuan lain yang masih di pipeline review + yang sudah DISBURSED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pengajuan dibuat, status awal MARKETING_REVIEW", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan berhasil dibuat\",\"data\":" + PENGAJUAN_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "400", description = "idBungaTenor/nominalPengajuan kosong", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST))),
            @ApiResponse(responseCode = "422", description = "Nominal melebihi sisa plafond", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Nominal pengajuan melebihi sisa plafond Anda (Rp4.000.000)\"}")))
    })
    public ApiResponse<PengajuanEntity> create(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer,
            @Valid @RequestBody PengajuanRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.create(currentCustomer.getId(), request);
        return ApiResponse.success(pengajuan, "Pengajuan berhasil dibuat");
    }

    @GetMapping("/me")
    @Operation(summary = "Riwayat pengajuan milik sendiri (customer)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Riwayat pengajuan berhasil diambil\",\"data\":[" + PENGAJUAN_EXAMPLE + "]}"))),
            @ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN)))
    })
    public ApiResponse<List<PengajuanEntity>> getMyPengajuan(
            @AuthenticationPrincipal AppCustomerEntity currentCustomer
    ) {
        List<PengajuanEntity> list = pengajuanService.getByCustomer(currentCustomer.getId());
        return ApiResponse.success(list, "Riwayat pengajuan berhasil diambil");
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Batalkan pengajuan sendiri (customer)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pengajuan dibatalkan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan berhasil dibatalkan\",\"data\":" + PENGAJUAN_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "422", description = "Pengajuan bukan milik customer ini, atau statusnya sudah tidak bisa dibatalkan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNPROCESSABLE_EMAIL_TAKEN)))
    })
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
    @Operation(summary = "Timeline review pengajuan (versi customer, terfilter)", description = "Beda dari /{id}/history (staff) - DTO ini gak bawa identitas staff internal, dan wajib cek pengajuan ini milik customer yang login.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Riwayat pengajuan berhasil diambil\",\"data\":[{\"action\":\"MARKETING_APPROVE\",\"statusFrom\":\"MARKETING_REVIEW\",\"statusTo\":\"BM_REVIEW\",\"catatan\":\"Dokumen lengkap\",\"roleName\":\"MARKETING\",\"createdAt\":\"2026-09-20T10:00:00\"}]}"))),
            @ApiResponse(responseCode = "422", description = "Pengajuan ini bukan milik customer yang login", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Anda tidak berhak mengakses riwayat pengajuan ini\"}")))
    })
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
    @Operation(summary = "Semua pengajuan, semua status (staff, read-only monitoring)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Semua pengajuan berhasil diambil\",\"data\":[" + PENGAJUAN_EXAMPLE + "]}"))),
            @ApiResponse(responseCode = "403", description = "Role gak punya akses (bukan staff)", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<PengajuanEntity>> getAll() {
        return ApiResponse.success(pengajuanService.getAll(), "Semua pengajuan berhasil diambil");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail 1 pengajuan (staff)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan berhasil diambil\",\"data\":" + PENGAJUAN_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "404", description = "ID tidak ditemukan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)))
    })
    public ApiResponse<PengajuanEntity> getById(@PathVariable UUID id) {
        return ApiResponse.success(pengajuanService.getById(id), "Pengajuan berhasil diambil");
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter pengajuan by status (dipakai queue per role di dashboard staff)", description = "status: MARKETING_REVIEW | MARKETING_REJECTED | BM_REVIEW | BM_REJECTED | BACKOFFICE_REVIEW | DISBURSED | CANCELLED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan berhasil difilter\",\"data\":[" + PENGAJUAN_EXAMPLE + "]}")))
    })
    public ApiResponse<List<PengajuanEntity>> getByStatus(@PathVariable String status) {
        return ApiResponse.success(pengajuanService.getByStatus(status), "Pengajuan berhasil difilter");
    }

    // === MARKETING actions ===

    @PatchMapping("/{id}/marketing-approve")
    @Operation(summary = "Approve tahap Marketing", description = "Lanjut ke status BM_REVIEW.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disetujui, lanjut ke BM", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan disetujui Marketing\",\"data\":" + PENGAJUAN_BM_REVIEW_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "403", description = "Bukan role MARKETING", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<PengajuanEntity> marketingApprove(
        @PathVariable UUID id,
        Authentication authentication,
        @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.marketingApprove(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan disetujui Marketing");
    }

    @PatchMapping("/{id}/marketing-reject")
    @Operation(summary = "Reject tahap Marketing", description = "catatan di body = alasan reject, disimpan ke ReviewLog.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ditolak", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan ditolak Marketing\",\"data\":" + PENGAJUAN_MARKETING_REJECTED_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "403", description = "Bukan role MARKETING", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
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
    @Operation(summary = "Approve tahap BM", description = "nominalDisetujui WAJIB diisi di body, dan tidak boleh lebih besar dari nominalPengajuan asli. Lanjut ke BACKOFFICE_REVIEW.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disetujui, lanjut ke Back Office", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan disetujui BM\",\"data\":" + PENGAJUAN_BACKOFFICE_REVIEW_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "400", description = "nominalDisetujui kosong atau melebihi nominalPengajuan", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"timestamp\":\"2026-09-21T10:15:30Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Nominal disetujui wajib diisi\"}")))
    })
    public ApiResponse<PengajuanEntity> bmApprove(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody PengajuanReviewRequest request
    ) {
        PengajuanEntity pengajuan = pengajuanService.bmApprove(id, authentication.getName(), request);
        return ApiResponse.success(pengajuan, "Pengajuan disetujui BM");
    }

    @PatchMapping("/{id}/bm-reject")
    @Operation(summary = "Reject tahap BM")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ditolak", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan ditolak BM\",\"data\":" + PENGAJUAN_BM_REJECTED_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "403", description = "Bukan role BM", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
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
    @Operation(summary = "Cairkan dana (Back Office)", description = "Tidak ada endpoint reject untuk Back Office - status akhir SELALU DISBURSED. Menstempel tanggalPencairan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dana dicairkan", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan berhasil dicairkan\",\"data\":" + PENGAJUAN_DISBURSED_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "403", description = "Bukan role BACK_OFFICE", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<PengajuanEntity> disburse(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        PengajuanEntity pengajuan = pengajuanService.disburse(id, authentication.getName());
        return ApiResponse.success(pengajuan, "Pengajuan berhasil dicairkan");
    }

    // === SUPERADMIN override ===

    @PatchMapping("/{id}/cancel-admin")
    @Operation(summary = "Force-cancel oleh superadmin", description = "Satu-satunya action state-changing yang superadmin boleh lakukan - sisanya read-only monitoring.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dibatalkan paksa", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"Pengajuan dibatalkan oleh admin\",\"data\":" + PENGAJUAN_CANCELLED_EXAMPLE + "}"))),
            @ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<PengajuanEntity> cancelBySuperadmin(@PathVariable UUID id) {
        return ApiResponse.success(pengajuanService.cancelBySuperadmin(id), "Pengajuan dibatalkan oleh admin");
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Timeline review pengajuan (versi staff, mentah)", description = "Balikin ReviewLogEntity apa adanya termasuk identitas staff yang review - beda dari /{id}/history/me punya customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"statusCode\":200,\"message\":\"History pengajuan berhasil diambil\",\"data\":[{\"id\":\"1a2b3c4d-...\",\"action\":\"MARKETING_APPROVE\",\"catatan\":\"Dokumen lengkap\",\"createdAt\":\"2026-09-20T10:00:00\",\"user\":{\"namaLengkap\":\"Dewi Marketing\"}}]}")))
    })
    public ApiResponse<List<ReviewLogEntity>> getHistory(@PathVariable UUID id) {
        List<ReviewLogEntity> history = reviewLogService.getHistoryByPengajuan(id);
        return ApiResponse.success(history, "History pengajuan berhasil diambil");
    }
}
