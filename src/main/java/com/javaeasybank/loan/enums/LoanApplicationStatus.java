package com.javaeasybank.loan.enums;

/**
 * 貸款申請狀態列舉。
 *
 * <p>記錄一筆貸款申請從建立到結案的完整生命週期，
 * 狀態轉移由 {@code LoanApplicationService} 負責驗證與執行。</p>
 *
 * <pre>
 * 典型流程：
 * PENDING_CONTACT → IN_CONTACT → PENDING_REVIEW → APPROVED → DISBURSED → CLOSED
 *                                               ↘ REJECTED
 *                               ↘ RETURNED（退回補件）
 * 任意階段可轉為 CANCELLED（客戶主動取消）
 * </pre>
 */
public enum LoanApplicationStatus {

    /** 待聯絡：申請剛建立，尚未有行員與客戶取得聯繫。 */
    PENDING_CONTACT,

    /** 聯繫中：行員已嘗試或成功聯繫客戶，正在確認申請意願。 */
    IN_CONTACT,

    /** 審核中：申請已進入風控審核流程，等待風控系統回調結果。 */
    PENDING_REVIEW,

    /** 退回補件：風控或行員要求客戶補充文件，待客戶重新送件。 */
    RETURNED,

    /** 已核准：風控審核通過，等待撥款至客戶指定帳戶。 */
    APPROVED,

    /** 已拒絕：風控審核未通過，申請終止。 */
    REJECTED,

    /** 已取消：客戶主動撤回申請，申請終止。 */
    CANCELLED,

    /** 已撥款：核准金額已匯入客戶指定帳戶，貸款帳戶同步建立。 */
    DISBURSED,

    /** 已結案：貸款全數還清或其他原因結案，流程完全終止。 */
    CLOSED
}
