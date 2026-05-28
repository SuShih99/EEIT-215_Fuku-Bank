package com.javaeasybank.loan.enums;

// 行員二次填單（審核詳情）狀態列舉
/**
 * 行員二次填單狀態。
 * 用於標示審核詳情仍為草稿或已送出風控審查。
 */
public enum LoanReviewStatus {

    DRAFT,  // 草稿
    SUBMITTED  // 已送審
}
