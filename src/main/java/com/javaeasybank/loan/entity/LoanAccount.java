package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanAccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
public class LoanAccount {

    @Id
    private String accountId;               // 帳戶ID

    private String applicationId;           // 申請編號
    private String customerId;              // 用戶ID

    // 撥款條件（從二次填單複製過來）
    private String applyType;               // 貸款種類
    private Long principalAmount;           // 核准撥款金額(本金)
    private Integer period;                 // 還款期數
    private BigDecimal rate;                // 年利率
    private BigDecimal monthlyPayment;      // 每期應繳金額

    // 還款進度
    private Integer paidPeriods;            // 已繳期數（初始為 0）
    private BigDecimal remainingPrincipal;  // 剩餘本金（初始等於 principalAmount）

    // 日期
    private LocalDate startDate;            // 撥款日（還款起算日）
    private LocalDate nextPaymentDate;      // 下次應繳日

    // ── 帳戶狀態 ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private LoanAccountStatus accountStatus; // account_status

    // ── 時間戳 ──────────────────────────────────────────────────────
    private LocalDateTime createTime;       // create_time
    private LocalDateTime updateTime;       // update_time（每次還款或狀態異動時更新）
}