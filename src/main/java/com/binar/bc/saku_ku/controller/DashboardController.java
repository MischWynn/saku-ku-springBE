package com.binar.bc.saku_ku.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.binar.bc.saku_ku.service.DashboardService;
import com.binar.bc.saku_ku.dto.ApiResponse;
import com.binar.bc.saku_ku.dto.DashboardSummaryResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/superadmin/summary")
    public ApiResponse<DashboardSummaryResponse> getSuperadminSummary() {
        return ApiResponse.success(dashboardService.getSuperadminSummary(), "Dashboard summary berhasil diambil");
    }

    @GetMapping("/superadmin/aktivitas")
    public ApiResponse<List<ReviewLogEntity>> getRecentActivity() {
        return ApiResponse.success(dashboardService.getRecentActivity(), "Aktivitas terbaru berhasil diambil");
    }
}