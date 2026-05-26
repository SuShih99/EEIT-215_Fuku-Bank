package com.javaeasybank.account.enums;

/**
 * 定義了系統支援的貨幣類型及其相關屬性，如小數位數和計息天數基準。
 */
public enum Currency {
    TWD(2, 365),    // 新台幣
    USD(2, 360),    // 美元
    EUR(2, 360),    // 歐元
    JPY(0, 360),    // 日圓
    GBP(2, 365),    // 英鎊
    CNY(2, 365),    // 人民幣
    AUD(2, 365),    // 澳幣
    CAD(2, 365),    // 加幣
    CHF(2, 360),    // 瑞士法郎
    HKD(2, 365);    // 港幣

    private final int decimalPlaces;
    private final int dayCountBasis;

    Currency(int decimalPlaces, int dayCountBasis) {
        this.decimalPlaces = decimalPlaces;
        this.dayCountBasis = dayCountBasis;
    }

    /**
     * 取得該幣別的小數位數
     * @return 小數位數 (例如 TWD 為 2, JPY 為 0)
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * 取得該幣別利息計算的年度天數基準
     * @return 天數基準 (360 或 365)
     */
    public int getDayCountBasis() {
        return dayCountBasis;
    }
}
