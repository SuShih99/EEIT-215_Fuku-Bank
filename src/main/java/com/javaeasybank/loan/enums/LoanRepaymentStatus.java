package com.javaeasybank.loan.enums;

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
