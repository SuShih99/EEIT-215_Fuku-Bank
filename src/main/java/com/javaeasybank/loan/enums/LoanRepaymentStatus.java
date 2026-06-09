package com.javaeasybank.loan.enums;

/**
 * 程式說明：
 * - 還款狀態列舉。
 * - 定義每期還款目前是未繳、已繳、逾期或其他帳務處理狀態。
 */

// 還款期數狀態列舉
/**
 * 還款期數狀態。
 * 用於分辨單一期數尚未到期、已繳清或已逾期。
 */
public enum LoanRepaymentStatus {

    SCHEDULED,  // 待繳
    PAID,  // 已繳清
    OVERDUE  // 逾期未繳
}
