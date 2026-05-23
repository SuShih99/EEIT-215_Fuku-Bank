package com.javaeasybank.risk.dto.response;

import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskReviewResponse {

    /**
     * 風險事件流水號（對應 RiskEventLog.logId）
     * 供來源模組存起來，後續查詢進度用
     */
    private Long logId;

    /**
     * 業務主鍵（原樣回傳，方便來源模組對應）
     */
    private String businessId;

    /**
     * 風控處置結果：PASS / REJECT / MANUAL_REVIEW
     * 對應你的 Disposition enum
     */
    private Disposition disposition;

    /**
     * 風險等級：LOW / MEDIUM / HIGH
     * 對應你的 RiskLevel enum
     */
    private RiskLevel riskLevel;

    /**
     * 最終分數（對應 CustomerCreditInfo.finalScore，0~100）
     * MANUAL_REVIEW 時也可回傳，讓來源模組知道是邊緣案件
     */
    private Integer finalScore;

    /**
     * 處置原因說明
     * PASS        → "自動審核通過"
     * REJECT      → "信用分數不足 / 命中黑名單 / ..."
     * MANUAL_REVIEW → "分數介於閾值區間，需人工複核"
     */
    private String reason;

    /**
     * 僅 MANUAL_REVIEW 時有值，其餘為 null
     * 來源模組可選擇存起來，用於追蹤人審任務
     */
    private Long reviewTaskId;
}

