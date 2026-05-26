package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanReviewStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 行員二次填單（審核詳情）查詢回應 DTO
@Getter
@Setter
public class LoanReviewDetailResponseDTO {

    // 審核詳情唯一識別碼（UUID）
    private String reviewId;

    // 關聯的貸款申請識別碼
    private String applicationId;

    // 行員確認的核准金額（新台幣）
    private BigDecimal confirmedAmount;

    // 行員確認的核准期數（月）
    private Integer confirmedPeriod;

    // 行員確認的核准年利率（百分比小數）
    private BigDecimal confirmedRate;

    // 擔保品備註說明（選填）
    private String collateralNote;

    // 填寫此份審核詳情的行員工號
    private String empId;

    // 最後儲存（草稿或送審）的時間戳記
    private LocalDateTime reviewTime;

    // 審核詳情的填寫狀態，參見 LoanReviewStatus（草稿 / 已送審）
    private LoanReviewStatus reviewStatus;

    // 正式送審的時間戳記
    private LocalDateTime submittedTime;

    // 行員審核備註，例如補充說明核准條件調整的原因（選填）
    private String reviewNote;
}
