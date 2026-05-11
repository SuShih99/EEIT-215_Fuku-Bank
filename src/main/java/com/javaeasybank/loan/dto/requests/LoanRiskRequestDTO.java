package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ===送給風控模組的審核資料===
// 整合 LoanApplication（原始申請）+ LoanReviewDetail（行員二次填單）
// 風控審完後透過 callbackUrl 打回 LoanCallbackController
@Getter
@Setter
public class LoanRiskRequestDTO {

    // 申請基本資料
    private String applicationId;
    private String customerId;   // 內部識別
    private String cif;          // 對外顯示用的顧客識別碼
    private String applyType;

    // 二次填單內容
    private BigDecimal confirmedAmount;
    private Integer confirmedPeriod;
    private BigDecimal confirmedRate;
    private String collateralNote;

    // 行員資訊
    private String empId;
    private LocalDateTime submittedTime;

    // 回傳設定
    // 由 LoanRiskClient 統一注入，呼叫方不需自行填寫
    private String callbackUrl;
}