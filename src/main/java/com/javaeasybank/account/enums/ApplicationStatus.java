package com.javaeasybank.account.enums;

/**
 * 開戶申請狀態
 */
public enum ApplicationStatus {
    PENDING,                // 待審核
    SUPPLEMENT_REQUIRED,    // 需補件
    APPROVED,               // 已核准
    REJECTED,               // 已駁回
    CANCELLED               // 已取消（客戶自行取消）
}
