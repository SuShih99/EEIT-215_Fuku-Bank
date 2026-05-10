package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExchangeRequest {

    @NotBlank(message = "轉出帳戶不可為空")
    private String fromAccountNumber;

    @NotBlank(message = "轉入帳戶不可為空")
    private String toAccountNumber;

    @NotNull(message = "換匯金額不可為空")
    @DecimalMin(value = "0.0001", message = "換匯金額必須大於 0")
    private BigDecimal fromAmount;

    private String note;
}
