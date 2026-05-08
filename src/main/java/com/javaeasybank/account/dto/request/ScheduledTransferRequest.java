package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScheduledTransferRequest {

    @NotBlank(message = "轉出帳號不可為空")
    private String fromAccountNumber;

    @NotBlank(message = "轉入帳號不可為空")
    private String toAccountNumber;

    @NotNull(message = "金額不可為空")
    @Positive(message = "金額必須為正數")
    private BigDecimal amount;

    @NotBlank(message = "預約日期不可為空")
    private String scheduledDate;

    private String note;
}
