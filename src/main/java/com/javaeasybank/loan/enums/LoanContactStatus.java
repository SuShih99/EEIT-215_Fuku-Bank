package com.javaeasybank.loan.enums;

// 聯繫結果狀態列舉
public enum LoanContactStatus {

    NOT_CONTACTED,  // 未聯繫
    ATTEMPTED,  // 已嘗試未聯繫上
    REACHED,  // 已接通
    CONFIRMED,  // 已確認繼續
    DECLINED  // 客戶放棄
}
