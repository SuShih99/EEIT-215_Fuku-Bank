package com.javaeasybank.account.enums;

/**
 * 定義了帳戶的類型。
 */
public enum AccountType {
    CHECKING,       // 活存
    SAVINGS,        // 儲蓄
    TIME_DEPOSIT,   // 定存
    LOAN,           // 貸款
    SUB_ACCOUNT,    // 子帳戶
    BUSINESS,       // 銀行業務帳戶
    CREDIT_CARD;    // 信用卡繳款帳戶

    public boolean isCustomerVisible() {
        return this != LOAN && this != CREDIT_CARD && this != BUSINESS;
    }

    public boolean isGeneralBalanceAccount() {
        return isCustomerVisible();
    }
}
