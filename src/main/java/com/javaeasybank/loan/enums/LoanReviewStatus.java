package com.javaeasybank.loan.enums;

/**
 * 程式說明：
 * - 審核結果列舉。
 * - 定義審核明細中的通過、退回、拒絕或待審等決策結果。
 */

// 行員二次填單（審核詳情）狀態列舉
/**
 * 行員二次填單狀態。
 * 用於標示審核詳情仍為草稿或已送出風控審查。
 */
public enum LoanReviewStatus {

    DRAFT,  // 草稿
    SUBMITTED  // 已送審
}
