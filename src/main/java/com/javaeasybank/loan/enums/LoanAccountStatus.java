package com.javaeasybank.loan.enums;

// 貸款帳戶狀態列舉
/**
 * 貸款帳戶狀態。
 * 描述撥款後帳戶目前是正常還款、逾期或已全數結清。
 */
public enum LoanAccountStatus {

    ACTIVE,  // 還款中
    OVERDUE,  // 存在逾期
    PAID_OFF,  // 已結清
}
