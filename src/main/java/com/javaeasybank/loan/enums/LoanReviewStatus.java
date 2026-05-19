package com.javaeasybank.loan.enums;

/**
 * 行員二次填單（審核詳情）狀態列舉。
 *
 * <p>描述 {@code LoanReviewDetail} 的填寫與送審狀態。
 * 行員可先儲存草稿（{@code DRAFT}），確認無誤後再正式送審（{@code SUBMITTED}），
 * 送審後系統會呼叫風控模組並將申請狀態切換為 {@code PENDING_REVIEW}。</p>
 */
public enum LoanReviewStatus {

    /** 草稿：行員已填寫部分或全部欄位，尚未正式送出審核。 */
    DRAFT,

    /** 已送審：行員確認送出，風控審核流程已啟動。 */
    SUBMITTED
}
