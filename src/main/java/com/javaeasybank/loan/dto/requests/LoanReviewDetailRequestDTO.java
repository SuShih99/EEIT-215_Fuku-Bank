package com.javaeasybank.loan.dto.requests;

/**
 * 程式說明：
 * - 貸款審核明細的請求資料物件。
 * - 保存審核人員輸入的風險分數、核准金額、利率、期數與審核意見。
 */

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 行員儲存或提交二次填單（審核詳情）的請求 DTO
/**
 * 行員二次填單請求 DTO。
 * 承載確認後的核准金額、期數、利率、審核意見與擔保品備註。
 */
@Getter
@Setter
public class LoanReviewDetailRequestDTO {

    // 行員確認的核准金額（新台幣），可與客戶申請金額不同
    private BigDecimal confirmedAmount;

    // 行員確認的核准期數（月）
    private Integer confirmedPeriod;

    // 行員確認的核准年利率（百分比小數）
    private BigDecimal confirmedRate;

    // 擔保品備註，說明擔保品的種類、估值或特殊條件（選填）
    private String collateralNote;

    // 填寫此份審核詳情的行員工號
    private String empId;
}
