package com.javaeasybank.risk.core;

import com.javaeasybank.risk.core.enums.BusinessScene;

public interface RiskProcessor {
    // 判斷這個 Handler 是否支援該場景
    BusinessScene getScene();

    // 審核通過後的邏輯 (例如：通知貸款模組撥款)
    void handleApproved(String businessId, String note);

    // 審核拒絕後的邏輯
    void handleRejected(String businessId, String note);
}