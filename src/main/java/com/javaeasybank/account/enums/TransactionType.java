package com.javaeasybank.account.enums;

/**
 * 定義了交易的類型。
 */
public enum TransactionType {
    TRANSFER,           // 轉帳
    TRANSFER_FEE,       // 轉帳手續費
    DEPOSIT,            // 存款
    WITHDRAW,           // 提款
    EXCHANGE,           // 換匯
    INTEREST,           // 利息入帳
    LOAN_DISBURSEMENT,  // 貸款撥款
    LOAN_REPAYMENT,     // 貸款還款
    REVERSAL            // 沖正
}
