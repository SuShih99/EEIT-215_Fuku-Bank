package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanAccountStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class LoanAccountResponseDTO {
    private String accountId;
    private String applicationId;
    private String customerId;

    // ── 核准條件 ─────────────────────────────────────────────────────
    private Long principalAmount;           // 核准金額
    private Integer period;                 // 總期數
    private BigDecimal rate;                // 年利率
    private BigDecimal monthlyPayment;      // 每期應繳金額

    // ── 還款進度 ─────────────────────────────────────────────────────
    private Integer paidPeriods;            // 已繳期數
    private BigDecimal remainingPrincipal;  // 剩餘本金
    private LocalDate startDate;            // 撥款日
    private LocalDate nextPaymentDate;      // 下次應繳日

    // ── 帳戶狀態 ─────────────────────────────────────────────────────
    private LoanAccountStatus accountStatus;
    private LocalDateTime createTime;

    // ── 還款明細（客戶端時間表用，可依需求決定是否帶入）────────────────
    private List<LoanRepaymentResponseDTO> repayments;
}