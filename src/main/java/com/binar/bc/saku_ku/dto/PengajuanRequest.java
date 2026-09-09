package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PengajuanRequest {

    @NotNull(message = "Bunga tenor tidak boleh kosong")
    private UUID idBungaTenor;

    @NotNull(message = "Nominal pengajuan tidak boleh kosong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Nominal pengajuan harus lebih dari 0")
    private BigDecimal nominalPengajuan;

    // Opsional dulu (Android customer app belum ada) — MODAL_USAHA/KONSUMTIF/PENDIDIKAN/KESEHATAN/RENOVASI/LAINNYA
    private String tujuanPinjaman;
}