package com.javaeasybank.loan.enums;

/**
 * 聯繫管道列舉。
 *
 * <p>記錄行員與客戶聯繫時所使用的溝通方式，
 * 供 {@code LoanContactLog} 儲存每次聯繫紀錄時使用。</p>
 */
public enum LoanContactChannel {

    /** 電話：行員以電話方式聯繫客戶。 */
    PHONE,

    /** 電子郵件：行員以 Email 方式聯繫客戶。 */
    EMAIL,

    /** 簡訊：行員以 SMS 簡訊方式聯繫客戶。 */
    SMS
}
