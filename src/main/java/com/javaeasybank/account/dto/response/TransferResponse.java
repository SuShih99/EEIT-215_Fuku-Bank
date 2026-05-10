package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轉帳響應 DTO，用於向客戶端返回轉帳結果資訊。
 */
@Data
public class TransferResponse {
    /**
     * 轉帳交易的參考 ID。
     */
    private String referenceId;
    /**
     * 轉出帳戶轉帳後的餘額。
     */
    private BigDecimal fromAccountBalance;
    /**
     * 轉入帳戶轉帳後的餘額。
     */
    private BigDecimal toAccountBalance;
    /**
     * 轉帳本金。
     */
    private BigDecimal amount;
    /**
     * 手續費。
     */
    private BigDecimal feeAmount;
    /**
     * 本次總扣款金額。
     */
    private BigDecimal totalDebitAmount;
    /**
     * 是否為跨行交易。
     */
    private boolean interbank;
    /**
     * 轉入銀行代碼。
     */
    private String toBankCode;
    /**
     * 轉入銀行名稱。
     */
    private String toBankName;
    /**
     * 轉帳完成的時間。
     */
    private LocalDateTime transferredAt;
}
