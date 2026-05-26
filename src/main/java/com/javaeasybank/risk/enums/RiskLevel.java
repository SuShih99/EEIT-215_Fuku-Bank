package com.javaeasybank.risk.enums;

public enum RiskLevel {

    SUSPENDED, // 拒絕往來/凍結
    LOW, // 低風險：正常交易
    MEDIUM, // 中風險：建議加強驗證（如簡訊 OTP）
    HIGH, // 高風險：可能觸發風控攔截或需人工核可
    CRITICAL // 極高風險：直接拒絕交易
}
