package com.binar.bc.saku_ku.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import com.binar.bc.saku_ku.repository.PengajuanRepository;
import com.binar.bc.saku_ku.repository.ReviewLogRepository;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.dto.DashboardSummaryResponse;
import com.binar.bc.saku_ku.dto.LoanTrendPoint;
import com.binar.bc.saku_ku.dto.StatusBreakdownItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PengajuanRepository pengajuanRepository;
    private final ReviewLogRepository reviewLogRepository;

    private static final List<String> ALL_STATUSES = List.of(
            "MARKETING_REVIEW", "BM_REVIEW", "BACKOFFICE_REVIEW",
            "DISBURSED", "MARKETING_REJECTED", "BM_REJECTED", "CANCELLED"
    );

    public DashboardSummaryResponse getSuperadminSummary() {
        return DashboardSummaryResponse.builder()
                .totalPengajuan(pengajuanRepository.count())
                .menungguReview(pengajuanRepository.countByStatus("MARKETING_REVIEW"))
                .menungguApproval(pengajuanRepository.countByStatus("BM_REVIEW"))
                .siapDicairkan(pengajuanRepository.countByStatus("BACKOFFICE_REVIEW"))
                .totalDanaCair(pengajuanRepository.sumNominalDisetujuiDisbursed())
                .loanTrend(buildLoanTrend())
                .statusBreakdown(buildStatusBreakdown())
                .build();
    }

    public List<ReviewLogEntity> getRecentActivity() {
        return reviewLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    private List<LoanTrendPoint> buildLoanTrend() {
        LocalDateTime startDate = LocalDate.now().minusDays(6).atStartOfDay();
        List<Object[]> raw = pengajuanRepository.countPengajuanPerDaySince(startDate);

        Map<LocalDate, Long> dataMap = raw.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        List<LoanTrendPoint> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(new LoanTrendPoint(date, dataMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    private List<StatusBreakdownItem> buildStatusBreakdown() {
        return ALL_STATUSES.stream()
                .map(status -> new StatusBreakdownItem(status, pengajuanRepository.countByStatus(status)))
                .filter(item -> item.getCount() > 0) // skip status kosong biar donut ga ada slice 0%
                .toList();
    }
}