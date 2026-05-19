package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 單期還款紀錄查詢回應 DTO。
 *
 * <p>撥款時由 {@code LoanRepaymentService} 依還款時間表預建所有期數，
 * 此 DTO 供前端顯示還款時間表或逐期明細查詢使用。
 * 金額拆分由 {@code AmortizationCalculator}（本息攤還法）計算。</p>
 */
@Getter
@Setter
public class LoanRepaymentResponseDTO {

    /** 還款紀錄唯一識別碼（UUID）。 */
    private String repaymentId;

    /** 關聯的貸款帳戶識別碼。 */
    private String accountId;

    /** 期數（1-based），第一期為 1，依此類推。 */
    private Integer periodIndex;

    /** 本期預計應繳截止日，逾期後排程會標記為 {@code OVERDUE}。 */
    private LocalDate scheduledDate;

    /**
     * 實際繳款成功的日期。
     * 若尚未繳款（{@code SCHEDULED} 或 {@code OVERDUE}）則為 {@code null}。
     */
    private LocalDate paidDate;

    /** 本期應繳總金額（本金 + 利息），單位：新台幣。 */
    private BigDecimal totalAmount;

    /** 本期應繳總額中的本金部分，單位：新台幣。 */
    private BigDecimal principalPortion;

    /** 本期應繳總額中的利息部分，單位：新台幣。 */
    private BigDecimal interestPortion;

    /** 繳清本期後的剩餘貸款本金，單位：新台幣。 */
    private BigDecimal remainingAfter;

    /** 本期還款狀態，參見 {@code LoanRepaymentStatus}。 */
    private LoanRepaymentStatus repaymentStatus;
}
