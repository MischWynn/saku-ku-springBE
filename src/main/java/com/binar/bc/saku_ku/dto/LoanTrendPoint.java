package com.binar.bc.saku_ku.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor

public class LoanTrendPoint {

    private LocalDate date;
    private long count;
}

