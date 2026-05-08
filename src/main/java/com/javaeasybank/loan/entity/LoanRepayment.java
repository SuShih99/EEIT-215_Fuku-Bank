package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_REPAYMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanRepayment {

    @Id
    private String repaymentId;             // 識別碼

    private String accountId;              // 帳戶ID

    // 期數資訊
    private Integer periodIndex;            // 期數序號
    private LocalDate scheduledDate;        // 預計應繳日（建立時預排）
    private LocalDate paidDate;             // 實際繳款日（SCHEDULED / OVERDUE 時為 null）

    // 金額明細
    private BigDecimal totalAmount;         // 本期應繳總額（本金 + 利息）
    private BigDecimal principalPortion;    // 本期本金部分
    private BigDecimal interestPortion;     // 本期利息部分
    private BigDecimal remainingAfter;      // 繳完後剩餘本金（建立時預算，還款後更新）

    // 狀態
    @Enumerated(EnumType.STRING)
    private LoanRepaymentStatus repaymentStatus; // 還款狀態

    // 時間戳
    private LocalDateTime createTime;
    private LocalDateTime updateTime;       // 還款時更新
}