package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanAccountCreateRequest {

    @NotBlank
    private String customerId;

    @NotNull
    @Positive
    private BigDecimal liability;

    @NotNull
    private BigDecimal rate;
}
