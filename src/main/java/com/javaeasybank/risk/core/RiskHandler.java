package com.javaeasybank.risk.core;

import java.math.BigDecimal;

public interface RiskHandler {
    // 判斷這個 Handler 是否支援該場景
    RiskScene getScene();

    // 執行具體的檢查邏輯
    void check(String identifier, BigDecimal amount);
}