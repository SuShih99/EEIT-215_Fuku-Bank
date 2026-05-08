package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanRepaymentResponseDTO {
    private String repaymentId;
    private String accountId;
    private Integer periodIndex;            // 第幾期（1-based）
    private LocalDate scheduledDate;        // 預計應繳日
    private LocalDate paidDate;             // 實際繳款日（未繳為 null）
    private BigDecimal totalAmount;         // 本期應繳總額
    private BigDecimal principalPortion;    // 本金部分
    private BigDecimal interestPortion;     // 利息部分
    private BigDecimal remainingAfter;      // 繳完後剩餘本金
    private LoanRepaymentStatus repaymentStatus;
}