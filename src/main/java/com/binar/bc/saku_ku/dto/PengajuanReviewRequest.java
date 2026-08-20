package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PengajuanReviewRequest {

    private String catatan; // opsional, alasan approve/reject — nanti dipakai juga di ReviewLog

    // Cuma diisi kalau ini approve dari BM (boleh beda dari nominal_pengajuan awal, tapi tidak boleh lebih besar)
    @DecimalMin(value = "0.0", inclusive = false, message = "Nominal disetujui harus lebih dari 0")
    private BigDecimal nominalDisetujui;
}