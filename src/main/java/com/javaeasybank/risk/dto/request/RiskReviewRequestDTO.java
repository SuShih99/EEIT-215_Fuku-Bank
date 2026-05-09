package com.javaeasybank.risk.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskReviewRequestDTO {
    private String callerModule;       // LOAN / CUSTOMER / ACCOUNT
    private String reviewType;         // LOAN_APPLICATION / KYC / ABNORMAL_TX
    private Long targetId;
    private String callbackUrl;

    // 共用
    private String applicantId;        // 身分證號 or customerId

    // 貸款用
    private String applicantPhone;
    private String applicantEmail;
    private BigDecimal requestedAmount;
    private Integer loanTermMonths;

    // KYC 用
    private String idDocumentUrl;

    // 異常交易用
    private BigDecimal transactionAmount;
    private String sourceIp;
    private String transactionType;
}