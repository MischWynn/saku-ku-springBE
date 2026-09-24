package com.binar.bc.saku_ku.service;

import com.binar.bc.saku_ku.dto.DashboardSummaryResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.repository.PengajuanRepository;
import com.binar.bc.saku_ku.repository.ReviewLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PengajuanRepository pengajuanRepository;
    @Mock
    private ReviewLogRepository reviewLogRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(pengajuanRepository, reviewLogRepository);
    }

    @Test
    void getSuperadminSummary_buildsFullSummary() {
        when(pengajuanRepository.count()).thenReturn(50L);
        when(pengajuanRepository.countByStatus(anyString())).thenReturn(3L);
        when(pengajuanRepository.sumNominalDisetujuiDisbursed()).thenReturn(new BigDecimal("100000000"));

        LocalDate today = LocalDate.now();
        List<Object[]> trendRows = java.util.Collections.singletonList(new Object[]{today, 5L});
        when(pengajuanRepository.countPengajuanPerDaySince(any(LocalDateTime.class))).thenReturn(trendRows);

        DashboardSummaryResponse result = service.getSuperadminSummary();

        assertThat(result.getTotalPengajuan()).isEqualTo(50L);
        assertThat(result.getTotalDanaCair()).isEqualByComparingTo("100000000");
        assertThat(result.getLoanTrend()).hasSize(7);
        assertThat(result.getLoanTrend().get(6).getCount()).isEqualTo(5L);
        // 7 statuses all stubbed to count 3 -> all included since filter is count > 0
        assertThat(result.getStatusBreakdown()).hasSize(7);
    }

    @Test
    void getSuperadminSummary_omitsZeroCountStatuses() {
        when(pengajuanRepository.count()).thenReturn(0L);
        when(pengajuanRepository.countByStatus(anyString())).thenReturn(0L);
        when(pengajuanRepository.sumNominalDisetujuiDisbursed()).thenReturn(BigDecimal.ZERO);
        when(pengajuanRepository.countPengajuanPerDaySince(any(LocalDateTime.class))).thenReturn(List.of());

        DashboardSummaryResponse result = service.getSuperadminSummary();

        assertThat(result.getStatusBreakdown()).isEmpty();
        assertThat(result.getLoanTrend()).hasSize(7);
        assertThat(result.getLoanTrend().get(0).getCount()).isZero();
    }

    @Test
    void getRecentActivity_delegatesToRepository() {
        ReviewLogEntity log = new ReviewLogEntity();
        when(reviewLogRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        assertThat(service.getRecentActivity()).containsExactly(log);
    }
}
