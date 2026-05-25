package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanAccountStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 貸款帳戶查詢回應 DTO
@Getter
@Setter
public class LoanAccountResponseDTO {

    // 貸款帳戶唯一識別碼（UUID）
    private String accountId;

    // 貸款帳號（對外顯示用，格式由帳戶模組定義）
    private String accountNumber;

    // 關聯的貸款申請識別碼，可追溯原始申請內容
    private String applicationId;

    // 客戶內部識別碼（系統內部使用，不對外顯示）
    private String customerId;

    // 客戶對外識別碼（CIF），前端顯示用
    private String cif;

    // 客戶姓名，前端列表直接顯示用
    private String memberName;

    // 貸款種類，例如 "PERSONAL"（信貸）、"HOUSE"（房貸）
    private String applyType;

    // 核准貸款金額（新台幣，單位
    private Long principalAmount;

    // 核准還款期數（月）
    private Integer confirmedPeriod;

    // 核准年利率（百分比小數，例如 0.05 代表 5%）
    private BigDecimal rate;

    // 每期（月）應繳總金額，由 AmortizationCalculator 計算
    private BigDecimal monthlyPayment;

    // 目前已繳清的期數
    private Integer paidPeriods;

    // 目前尚未還清的本金餘額（新台幣）
    private BigDecimal remainingPrincipal;

    // 撥款日（第一期起算日）
    private LocalDate startDate;

    // 下一期應繳款截止日，逾期後排程會自動標記為 OVERDUE
    private LocalDate nextPaymentDate;

    // 帳戶目前還款狀態，參見 LoanAccountStatus
    private LoanAccountStatus accountStatus;

    // 帳戶建立時間（即撥款時間）
    private LocalDateTime createTime;

    // 各期還款明細清單（依期數升序排列）
    private List<LoanRepaymentResponseDTO> repayments;
}
