package com.binar.bc.saku_ku.controller;

import com.binar.bc.saku_ku.dto.DashboardSummaryResponse;
import com.binar.bc.saku_ku.entity.ReviewLogEntity;
import com.binar.bc.saku_ku.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(dashboardService);
    }

    @Test
    void getSuperadminSummary_delegatesToService() {
        DashboardSummaryResponse summary = DashboardSummaryResponse.builder().totalPengajuan(10).build();
        when(dashboardService.getSuperadminSummary()).thenReturn(summary);

        assertThat(controller.getSuperadminSummary().getData()).isSameAs(summary);
    }

    @Test
    void getRecentActivity_delegatesToService() {
        ReviewLogEntity log = new ReviewLogEntity();
        when(dashboardService.getRecentActivity()).thenReturn(List.of(log));

        assertThat(controller.getRecentActivity().getData()).containsExactly(log);
    }
}
