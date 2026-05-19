package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 送往風控模組的審核請求 DTO。
 *
 * <p>整合 {@code LoanApplication}（客戶原始申請）與 {@code LoanReviewDetail}（行員二次填單）
 * 的資料，由 {@code LoanRiskClient} 組裝後呼叫風控系統 API。
 * 風控審核完成後，系統會透過 {@code callbackUrl} 回呼
 * {@code LoanCallbackController}，以更新申請狀態。</p>
 */
@Getter
@Setter
public class LoanRiskRequestDTO {

    // ── 申請基本資料 ─────────────────────────────────────────────────

    /** 貸款申請唯一識別碼，對應 {@code LoanApplication.applicationId}。 */
    private String applicationId;

    /** 客戶內部識別碼（系統內部使用，不對外顯示）。 */
    private String customerId;

    /** 客戶對外識別碼（CIF），供風控系統查詢客戶信用資料使用。 */
    private String cif;

    /** 貸款種類，例如 {@code "PERSONAL"}、{@code "HOUSE"}。 */
    private String applyType;

    // ── 行員二次填單確認值 ────────────────────────────────────────────

    /** 行員確認的核准金額（新台幣）。 */
    private BigDecimal confirmedAmount;

    /** 行員確認的核准期數（月）。 */
    private Integer confirmedPeriod;

    /** 行員確認的核准年利率（百分比小數）。 */
    private BigDecimal confirmedRate;

    /** 擔保品備註說明（選填）。 */
    private String collateralNote;

    // ── 行員資訊 ─────────────────────────────────────────────────────

    /** 送審行員工號。 */
    private String empId;

    /** 行員送出審核的時間戳記。 */
    private LocalDateTime submittedTime;

    // ── 風控回調設定 ──────────────────────────────────────────────────

    /**
     * 風控系統完成審核後回呼的 URL，指向 {@code LoanCallbackController}。
     * 此欄位由 {@code LoanRiskClient} 統一注入，呼叫方無需手動填寫。
     */
    private String callbackUrl;

    /** 業務場景識別碼，固定帶 {@code "LOAN_APPLY"}。 */
    private String scene;

    /** 業務單號，對應 {@code applicationId}，供風控系統追蹤使用。 */
    private String businessId;

    /** 審核金額，對應 {@code confirmedAmount}，供風控系統使用的別名欄位。 */
    private BigDecimal amount;
}
