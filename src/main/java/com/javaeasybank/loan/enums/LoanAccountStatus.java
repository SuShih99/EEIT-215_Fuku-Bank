package com.javaeasybank.loan.enums;

/**
 * 程式說明：
 * - 貸款帳戶狀態列舉。
 * - 用固定狀態表示帳戶啟用、結清、逾期或其他帳務生命週期階段。
 */

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
