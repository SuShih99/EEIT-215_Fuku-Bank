package com.javaeasybank.loan.dto.requests;

/**
 * 程式說明：
 * - 會員送出貸款申請時使用的請求資料物件。
 * - 承載申請類型、金額、期數、利率與撥款帳戶等前端輸入資料。
 */

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 客戶送出貸款申請時的請求 DTO。
 * 承載貸款種類、申請金額、期數、利率與撥款入帳帳號。
 */
@Getter
@Setter
public class LoanMemberRequestDTO {

    // 貸款種類，例如 "PERSONAL"（信貸）、"CAR"（車貸）、"HOUSE"（房貸）
    private String applyType;

    // 客戶申請金額（新台幣），須大於零
    private BigDecimal applyAmount;

    // 客戶申請期數（月），例如 12、24、36
    private Integer applyPeriod;

    // 申請時顯示的年利率（百分比小數，例如 0.05 代表 5%）
    private BigDecimal rate;

    // 客戶選擇的撥款入帳帳號（台幣活存帳號）
    private String disbursementAccount;
}
