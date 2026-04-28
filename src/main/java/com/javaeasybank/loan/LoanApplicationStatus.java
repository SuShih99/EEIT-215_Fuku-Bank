package com.javaeasybank.loan;

public enum LoanApplicationStatus {
    PENDING_CONTACT,    // 待聯絡
    IN_CONTACT,         // 聯繫中
    PENDING_REVIEW,     // 審核中
    APPROVED,           // 已核准
    REJECTED,           // 已拒絕
    CANCELLED,          // 已取消
    DISBURSED,          // 已撥款
    CLOSED              // 已結案
}

