package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanDisbursementRequest {

    // 對應 Loan 模組的申請編號（選填）
    // 提供時，撥款成功後自動通知 Loan 模組更新狀態並建立還款時間表
    private String applicationId;

    @NotBlank
    private String loanAccountNumber;

    @NotBlank
    private String toAccountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String note;
}
