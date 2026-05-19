package com.javaeasybank.loan.enums;

/**
 * 聯繫結果狀態列舉。
 *
 * <p>描述行員每次聯繫客戶後的結果，
 * 儲存於 {@code LoanContactLog} 的 {@code contactStatus} 欄位。
 * 申請層級的「最後聯繫狀態」亦從最新一筆聯繫紀錄中取得。</p>
 */
public enum LoanContactStatus {

    /** 未聯繫：尚未有任何聯繫嘗試。 */
    NOT_CONTACTED,

    /** 已嘗試未聯繫上：行員已撥打或發送訊息，但未獲回應。 */
    ATTEMPTED,

    /** 已接通：成功與客戶取得聯繫，但尚未確認後續意願。 */
    REACHED,

    /** 已確認繼續：客戶確認願意繼續進行貸款申請流程。 */
    CONFIRMED,

    /** 客戶放棄：客戶表示不願繼續，申請可轉為 {@code CANCELLED}。 */
    DECLINED
}
