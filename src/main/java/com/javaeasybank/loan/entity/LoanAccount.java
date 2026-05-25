package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanAccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 貸款帳戶 Entity，對應資料庫 loan_account
@Entity
@Table(name = "loan_account")
@Getter
@Setter
@NoArgsConstructor
public class LoanAccount {

    // 帳戶唯一識別碼（UUID），作為主鍵
    @Id
    private String accountId;

    // 貸款帳號（對外識別號，由帳戶模組產生並於撥款回調時帶入）
    private String accountNumber;

    // 關聯的貸款申請識別碼，可追溯原始申請
    private String applicationId;

    // 客戶內部識別碼（系統內部使用，不對外顯示）
    private String customerId;

    // 貸款種類，例如 "PERSONAL"（信貸）、"HOUSE"（房貸）
    private String applyType;

    // 核准撥款本金（新台幣，單位
    private Long principalAmount;

    // 核准還款期數（月）
    private Integer confirmedPeriod;

    // 核准年利率（百分比小數，例如 0.05 代表 5%）
    private BigDecimal rate;

    // 每期（月）應繳總金額，由 AmortizationCalculator 計算
    private BigDecimal monthlyPayment;

    // 目前已繳清的期數，初始為 0，每次繳款後 +1
    private Integer paidPeriods;

    // 目前尚未還清的本金餘額（新台幣）
    private BigDecimal remainingPrincipal;

    // 撥款日，同時作為還款起算日（第一期應繳日從此日起算一個月）
    private LocalDate startDate;

    // 下一期的應繳截止日，繳款後更新為下一期的 scheduledDate
    private LocalDate nextPaymentDate;

    // 帳戶還款狀態，以字串形式存入 DB，參見 LoanAccountStatus
    @Enumerated(EnumType.STRING)
    private LoanAccountStatus accountStatus;

    // 帳戶建立時間（即撥款時間）
    private LocalDateTime createTime;

    // 最後更新時間，每次繳款或狀態異動時更新
    private LocalDateTime updateTime;
}
