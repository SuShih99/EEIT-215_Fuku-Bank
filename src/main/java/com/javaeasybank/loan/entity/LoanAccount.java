package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanAccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 貸款帳戶 Entity，對應資料庫 loan_account。
 * 代表撥款後正式成立的貸款帳戶，保存本金、期數、利率、還款進度與帳戶狀態。
 */
@Entity
@Table(name = "loan_account")
@Getter
@Setter
@NoArgsConstructor
public class LoanAccount {

    @Id
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "account_number", length = 14)
    private String accountNumber;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "apply_type", length = 50)
    private String applyType;

    @Column(name = "principal_amount")
    private Long principalAmount;

    @Column(name = "confirmed_period")
    private Integer confirmedPeriod;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;

    @Column(name = "paid_periods", nullable = false)
    private Integer paidPeriods;

    @Column(name = "remaining_principal")
    private BigDecimal remainingPrincipal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private LoanAccountStatus accountStatus;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
