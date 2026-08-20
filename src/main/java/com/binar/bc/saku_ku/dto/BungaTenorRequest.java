package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BungaTenorRequest {

    @NotNull(message = "Tenor cannot be null/empty")
    @Positive(message = "Tenor must be a positive number")
    private Integer tenor;

    @NotNull(message = "Interest Rate cannot be null/empty")
    @DecimalMin(value = "0.0", inclusive = true, message = "Interest Rate should be greater than or equal to 0")
    private BigDecimal interestRate;

    // ACTIVE / INACTIVE, opsional saat create (default ACTIVE)
    private String status;
}