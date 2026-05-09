package com.javaeasybank.risk.dto.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskReviewRequest {
    // --- 公共欄位 (所有模組通用) ---
    private String businessId;      // 原 applicationId，改名為業務 ID (貸款單號/卡片申請號)
    private String businessType;    // 業務類型：LOAN, CREDIT_CARD, ACCOUNT_OPEN, TRANSFER
    private String customerId;      // 客戶 ID
    private String callbackUrl;     // 風控完畢後打回來的地址

    // --- 擴展欄位 (針對不同業務的差異資料) ---
    // 方案 A：使用 Map 存放不同業務的參數 (靈活性最高)
    private Map<String, Object> businessDetails;

    private LocalDateTime requestTime;
}
