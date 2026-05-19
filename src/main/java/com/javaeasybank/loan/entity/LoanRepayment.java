package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 還款期數 Entity，對應資料庫 {@code LOAN_REPAYMENT}。
 *
 * <p>撥款時由 {@code LoanRepaymentService} 依 {@code AmortizationCalculator}
 * 計算的攤還表，一次預建所有期數（初始狀態皆為 {@code SCHEDULED}）。
 * 後續由排程任務 {@code LoanRepaymentScheduler} 定期掃描並更新狀態。</p>
 */
@Entity
@Table(name = "LOAN_REPAYMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanRepayment {

    /** 還款期數唯一識別碼（UUID），作為主鍵。 */
    @Id
    private String repaymentId;

    /** 關聯的貸款帳戶識別碼。 */
    private String accountId;

    // ── 期數資訊 ─────────────────────────────────────────────────────

    /** 期數序號（1-based），第一期為 {@code 1}。 */
    private Integer periodIndex;

    /** 預計應繳截止日，建立時依攤還表預排，到期未繳則由排程標記為 {@code OVERDUE}。 */
    private LocalDate scheduledDate;

    /**
     * 實際繳款日。
     * 狀態為 {@code SCHEDULED} 或 {@code OVERDUE} 時此欄位為 {@code null}，
     * 繳款成功後由 {@code LoanRepaymentService} 寫入當下日期。
     */
    private LocalDate paidDate;

    // ── 金額明細（由 AmortizationCalculator 預算，還款後更新） ────────

    /** 本期應繳總額（本金 + 利息），單位：新台幣。 */
    private BigDecimal totalAmount;

    /** 本期應繳總額中的本金部分，單位：新台幣。 */
    private BigDecimal principalPortion;

    /** 本期應繳總額中的利息部分，單位：新台幣。 */
    private BigDecimal interestPortion;

    /**
     * 繳清本期後的預計剩餘本金，單位：新台幣。
     * 建立時由攤還表預算；實際繳款後由 {@code LoanRepaymentService} 確認更新。
     */
    private BigDecimal remainingAfter;

    // ── 狀態 ─────────────────────────────────────────────────────────

    /**
     * 本期還款狀態，以字串形式存入 DB。
     * 參見 {@code LoanRepaymentStatus}。
     */
    @Enumerated(EnumType.STRING)
    private LoanRepaymentStatus repaymentStatus;

    // ── 時間戳 ──────────────────────────────────────────────────────

    /** 此筆還款紀錄的建立時間（撥款時批次寫入）。 */
    private LocalDateTime createTime;

    /** 最後更新時間，繳款成功或排程標記逾期時更新。 */
    private LocalDateTime updateTime;
}
