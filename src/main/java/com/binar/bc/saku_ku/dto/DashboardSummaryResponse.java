package com.binar.bc.saku_ku.dto;

import com.binar.bc.saku_ku.dto.LoanTrendPoint;
import com.binar.bc.saku_ku.dto.StatusBreakdownItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder

public class DashboardSummaryResponse {
    private long totalPengajuan;
    private long menungguReview;
    private long menungguApproval;
    private long siapDicairkan;
    private BigDecimal totalDanaCair;
    private List<LoanTrendPoint> loanTrend;
    private List<StatusBreakdownItem> statusBreakdown;
}