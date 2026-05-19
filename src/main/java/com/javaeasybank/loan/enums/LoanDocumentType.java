package com.javaeasybank.loan.enums;

/**
 * 貸款申請文件類型列舉。
 *
 * <p>定義客戶於補件階段可上傳的文件種類，
 * 儲存於 {@code LoanDocument} 的 {@code documentType} 欄位，
 * 並於送審時一併傳遞給風控模組進行審核。</p>
 */
public enum LoanDocumentType {

    /** 身分證：客戶的中華民國國民身分證正反面。 */
    ID_CARD,

    /** 收入證明：薪資證明、所得稅申報等收入相關文件。 */
    INCOME_CERT,

    /** 在職證明：雇主出具的在職服務證明書。 */
    EMPLOYMENT_CERT,

    /** 銀行存摺：近三至六個月的存款交易明細。 */
    BANK_STATEMENT,

    /** 不動產謄本：地政機關核發的土地或建物登記謄本（擔保品用）。 */
    PROPERTY_CERT,

    /** 所有權狀：不動產所有權狀影本（擔保品用）。 */
    TITLE_DEED,

    /** 其他：不屬於以上類型的補充文件。 */
    OTHER
}
