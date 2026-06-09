package com.javaeasybank.loan.enums;

/**
 * 程式說明：
 * - 聯絡結果列舉。
 * - 定義聯絡成功、未接、拒絕或需要再次聯絡等處理結果。
 */

// 聯繫結果狀態列舉
/**
 * 聯繫結果狀態。
 * 用於描述行員聯繫客戶後的最新進度，例如已聯繫、待回覆或無法聯繫。
 */
public enum LoanContactStatus {

    NOT_CONTACTED,  // 未聯繫
    ATTEMPTED,  // 已嘗試未聯繫上
    REACHED,  // 已接通
    CONFIRMED,  // 已確認繼續
    DECLINED  // 客戶放棄
}
