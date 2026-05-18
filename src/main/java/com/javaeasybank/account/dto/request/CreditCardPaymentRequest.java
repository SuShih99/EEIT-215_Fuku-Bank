package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditCardPaymentRequest {

    private Integer billId;

    @NotBlank
    private String creditCardAccountNumber;

    @NotBlank
    private String fromAccountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String note;
}
