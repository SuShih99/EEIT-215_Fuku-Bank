package com.javaeasybank.loan.enums;

// 貸款申請狀態列舉
/**
 * 貸款申請狀態。
 * 描述案件從待聯繫、審核、補件、核准、撥款到結案的主流程。
 */
public enum LoanApplicationStatus {

    PENDING_CONTACT,  // 待聯絡
    IN_CONTACT,  // 聯繫中
    PENDING_REVIEW,  // 審核中
    RETURNED,  // 退回補件
    APPROVED,  // 已核准
    REJECTED,  // 已拒絕
    CANCELLED,  // 已取消
    DISBURSED,  // 已撥款
    CLOSED  // 已結案
}
