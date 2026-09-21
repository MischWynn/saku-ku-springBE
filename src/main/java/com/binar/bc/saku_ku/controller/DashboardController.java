package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.config.swagger.SwaggerExamples;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.binar.bc.saku_ku.service.DashboardService;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.DashboardSummaryResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard - Superadmin", description = "Statistik ringkas & aktivitas terbaru buat halaman Overview superadmin. Superadmin only.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/superadmin/summary")
    @Operation(summary = "Ringkasan dashboard", description = "Stat cards + loan trend 7 hari + status breakdown.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<DashboardSummaryResponse> getSuperadminSummary() {
        return ApiResponse.success(dashboardService.getSuperadminSummary(), "Dashboard summary berhasil diambil");
    }

    @GetMapping("/superadmin/aktivitas")
    @Operation(summary = "Aktivitas terbaru", description = "10 review log terbaru dari semua staff (nested pengajuan + customer).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Belum login", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED_TOKEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bukan role SUPERADMIN", content = @Content(
                    mediaType = "application/json", examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)))
    })
    public ApiResponse<List<ReviewLogEntity>> getRecentActivity() {
        return ApiResponse.success(dashboardService.getRecentActivity(), "Aktivitas terbaru berhasil diambil");
    }
}
