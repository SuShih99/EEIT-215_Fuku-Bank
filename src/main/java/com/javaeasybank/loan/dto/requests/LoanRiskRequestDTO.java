package com.javaeasybank.loan.dto.requests;

/**
 * 程式說明：
 * - 送往風控評分服務的請求資料物件。
 * - 彙整客戶財務資料與貸款條件，供風控模型計算風險等級或建議結果。
 */

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 送往風控模組的審核請求 DTO
/**
 * 貸款送風控請求 DTO。
 * 匯整申請資料、行員確認條件、補件文件與回調 URL，供風控模組審核使用。
 */
@Getter
@Setter
public class LoanRiskRequestDTO {

    // ── 申請基本資料 ─────────────────────────────────────────────────

    // 貸款申請唯一識別碼，對應 LoanApplication.applicationId
    private String applicationId;

    // 客戶內部識別碼（系統內部使用，不對外顯示）
    private String customerId;

    // 客戶對外識別碼（CIF），供風控系統查詢客戶信用資料使用
    private String cif;

    // 貸款種類，例如 "PERSONAL"、"HOUSE"
    private String applyType;

    // ── 行員二次填單確認值 ────────────────────────────────────────────

    // 行員確認的核准金額（新台幣）
    private BigDecimal confirmedAmount;

    // 行員確認的核准期數（月）
    private Integer confirmedPeriod;

    // 行員確認的核准年利率（百分比小數）
    private BigDecimal confirmedRate;

    // 擔保品備註說明（選填）
    private String collateralNote;

    // 客戶在聯繫階段已送出的申請文件清單（選填）
    private List<LoanDocumentInfoDTO> documents;

    // ── 行員資訊 ─────────────────────────────────────────────────────

    // 送審行員工號
    private String empId;

    // 行員送出審核的時間戳記
    private LocalDateTime submittedTime;

    // ── 風控回調設定 ──────────────────────────────────────────────────

    // 風控系統完成審核後回呼的 URL，指向 LoanCallbackController
    private String callbackUrl;

    // 業務場景識別碼，固定帶 "LOAN_APPLY"
    private String scene;

    // 業務單號，對應 applicationId，供風控系統追蹤使用
    private String businessId;

    // 審核金額，對應 confirmedAmount，供風控系統使用的別名欄位
    private BigDecimal amount;
}
