package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 行員儲存或提交二次填單（審核詳情）的請求 DTO。
 *
 * <p>行員在聯繫客戶確認意願後，針對申請進行條件調整（金額、期數、利率等），
 * 可先儲存為草稿或直接送審。
 * 對應 {@code POST /api/staff/loan-applications/{id}/review-detail}。</p>
 */
@Getter
@Setter
public class LoanReviewDetailRequestDTO {

    /**
     * 行員確認的核准金額（新台幣），可與客戶申請金額不同。
     * 例如客戶申請 100 萬，行員依風險評估調整為 80 萬。
     */
    private BigDecimal confirmedAmount;

    /** 行員確認的核准期數（月）。 */
    private Integer confirmedPeriod;

    /** 行員確認的核准年利率（百分比小數）。 */
    private BigDecimal confirmedRate;

    /**
     * 擔保品備註，說明擔保品的種類、估值或特殊條件（選填）。
     * 適用於房貸、車貸等需要擔保品的貸款種類。
     */
    private String collateralNote;

    /** 填寫此份審核詳情的行員工號。 */
    private String empId;
}
