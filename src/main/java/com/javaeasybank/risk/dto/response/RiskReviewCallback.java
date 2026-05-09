package com.javaeasybank.risk.dto.response;

public class RiskReviewCallback {
    private String businessId;      // 原路回傳業務單號
    private String result;          // APPROVED, REJECTED
    private String riskLevel;       // LOW, MEDIUM, HIGH (選填，供業務端參考)
    private String note;            // 審核意見或拒絕理由
    private String signature;       // 可選：數位簽章，防止業務端偽造風控結果
}
