package com.javaeasybank.loan.enums;

/**
 * 程式說明：
 * - 貸款文件類型列舉。
 * - 統一控管身分證明、收入證明、合約或其他貸款流程需要的文件種類。
 */

// 貸款申請文件類型列舉
/**
 * 補件文件類型。
 * 定義客戶可上傳及風控可要求補充的文件分類。
 */
public enum LoanDocumentType {

    ID_CARD,  // 身分證
    INCOME_CERT,  // 收入證明
    EMPLOYMENT_CERT,  // 在職證明
    BANK_STATEMENT,  // 銀行存摺
    PROPERTY_CERT,  // 不動產謄本
    TITLE_DEED,  // 所有權狀
    OTHER  // 其他
}
