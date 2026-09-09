package com.binar.bc.saku_ku.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlafondRequest {
    private String namaPlafond;
    private String deskripsi;
    private String tipe;
    private BigDecimal limitMaksimal;
    private String status;
}
