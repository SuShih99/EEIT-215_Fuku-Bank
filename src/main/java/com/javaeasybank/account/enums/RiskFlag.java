package com.javaeasybank.account.enums;

/**
 * 風險標記
 */
public enum RiskFlag {
    NORMAL,             // 正常
    WATCH,              // 觀察中
    PEP,                // 重要政治性職務之人
    HIGH_RISK,          // 高風險
    HIGH_FREQUENCY,     // 同源高頻申請
    PEP_HIGH_FREQUENCY  // 雙重風險
}
