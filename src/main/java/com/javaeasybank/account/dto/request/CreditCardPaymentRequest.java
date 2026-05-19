package com.javaeasybank.account.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreditCardPaymentRequest {

    private Integer billId;

    private List<Integer> billIds;

    @NotBlank
    private String creditCardAccountNumber;

    @NotBlank
    private String fromAccountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String note;
}
