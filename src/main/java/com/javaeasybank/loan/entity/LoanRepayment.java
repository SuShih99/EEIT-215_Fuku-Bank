package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 還款期數 Entity，對應資料庫 LOAN_REPAYMENT
@Entity
@Table(name = "LOAN_REPAYMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanRepayment {

    // 還款期數唯一識別碼（UUID），作為主鍵
    @Id
    private String repaymentId;

    // 關聯的貸款帳戶識別碼
    private String accountId;

    // 期數序號（1-based），第一期為 1
    private Integer periodIndex;

    // 預計應繳截止日，建立時依攤還表預排，到期未繳則由排程標記為 OVERDUE
    private LocalDate scheduledDate;

    // 實際繳款日
    private LocalDate paidDate;

    // 本期應繳總額（本金 + 利息），單位
    private BigDecimal totalAmount;

    // 本期應繳總額中的本金部分，單位
    private BigDecimal principalPortion;

    // 本期應繳總額中的利息部分，單位
    private BigDecimal interestPortion;

    // 繳清本期後的預計剩餘本金，單位：新台幣
    private BigDecimal remainingAfter;

    // 本期還款狀態，以字串形式存入 DB
    @Enumerated(EnumType.STRING)
    private LoanRepaymentStatus repaymentStatus;

    // 此筆還款紀錄的建立時間（撥款時批次寫入）
    private LocalDateTime createTime;

    // 最後更新時間，繳款成功或排程標記逾期時更新
    private LocalDateTime updateTime;
}
