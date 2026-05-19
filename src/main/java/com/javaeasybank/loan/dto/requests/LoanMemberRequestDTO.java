package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 客戶提交貸款申請的請求 DTO。
 *
 * <p>客戶於前端填寫申請表後送出，
 * 對應 {@code POST /api/customer/loan-applications}。
 * 申請建立後狀態預設為 {@code PENDING_CONTACT}，等待行員聯繫。</p>
 */
@Getter
@Setter
public class LoanMemberRequestDTO {

    /**
     * 貸款種類，例如 {@code "PERSONAL"}（信貸）、{@code "CAR"}（車貸）、{@code "HOUSE"}（房貸）。
     * 對應後端 applyType 欄位，影響後續風控審核規則。
     */
    private String applyType;

    /** 客戶申請金額（新台幣），須大於零。 */
    private BigDecimal applyAmount;

    /** 客戶申請期數（月），例如 12、24、36。 */
    private Integer applyPeriod;

    /** 申請時顯示的年利率（百分比小數，例如 {@code 0.05} 代表 5%）。 */
    private BigDecimal rate;

    /**
     * 客戶選擇的撥款入帳帳號（台幣活存帳號）。
     * 核准後，撥款作業將匯款至此帳號。
     * 帳號清單來源：{@code GET /api/customer/loan-repayments/debit-accounts}。
     */
    private String disbursementAccount;
}
