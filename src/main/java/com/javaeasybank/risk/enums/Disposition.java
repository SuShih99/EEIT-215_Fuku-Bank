package com.javaeasybank.risk.enums;

public enum Disposition {
    PASS,           // 正常放行
    REJECT,         // 直接拒絕 (拋出 Exception)
    MANUAL_REVIEW,  // 進入人工審核
    RETURN,
    WARNING

}
