package com.javaeasybank.loan.dto.response;

/**
 * 程式說明：
 * - 貸款還款紀錄查詢回應資料物件。
 * - 提供每期應繳本金、利息、總額、到期日與繳款狀態給前端或後台顯示。
 */

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// 單期還款紀錄查詢回應 DTO
/**
 * 還款期數回應 DTO。
 * 對外呈現單一期數的應繳日、實繳日、本金、利息、剩餘本金與狀態。
 */
@Getter
@Setter
public class LoanRepaymentResponseDTO {

    // 還款紀錄唯一識別碼（UUID）
    private String repaymentId;

    // 關聯的貸款帳戶識別碼
    private String accountId;

    // 期數（1-based），第一期為 1，依此類推
    private Integer periodIndex;

    // 本期預計應繳截止日，逾期後排程會標記為 OVERDUE
    private LocalDate scheduledDate;

    // 實際繳款成功的日期
    private LocalDate paidDate;

    // 本期應繳總金額（本金 + 利息），單位
    private BigDecimal totalAmount;

    // 本期應繳總額中的本金部分，單位
    private BigDecimal principalPortion;

    // 本期應繳總額中的利息部分，單位
    private BigDecimal interestPortion;

    // 繳清本期後的剩餘貸款本金，單位
    private BigDecimal remainingAfter;

    // 本期還款狀態，參見 LoanRepaymentStatus
    private LoanRepaymentStatus repaymentStatus;
}
