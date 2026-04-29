package com.javaeasybank.risk.core;

public enum RiskDisposition {
    PASS,           // 正常放行
    REJECT,         // 直接拒絕 (拋出 Exception)
    REVIEW,         // 進入人工審核
}
