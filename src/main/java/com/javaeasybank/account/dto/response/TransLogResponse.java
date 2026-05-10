package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.entity.TransLog;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易紀錄響應 DTO，用於向客戶端返回交易紀錄資訊。
 */
@Data
public class TransLogResponse {
    /**
     * 交易 ID。
     */
    private String transactionId;
    /**
     * 交易參考 ID。
     */
    private String referenceId;
    /**
     * 交易帳號。
     */
    private String accountNumber;
    /**
     * 對手方帳號。
     */
    private String counterpartAccount;
    /**
     * 本筆交易所屬銀行代碼。
     */
    private String bankCode;
    /**
     * 本筆交易所屬銀行名稱。
     */
    private String bankName;
    /**
     * 對手方銀行代碼。
     */
    private String counterpartBankCode;
    /**
     * 對手方銀行名稱。
     */
    private String counterpartBankName;
    /**
     * 是否跨行。
     */
    private boolean interbank;
    /**
     * 記帳類型 (DEBIT 或 CREDIT)。
     */
    private String entryType;
    /**
     * 交易類型 (例如 TRANSFER, DEPOSIT 等)。
     */
    private String transactionType;
    /**
     * 交易金額。
     */
    private BigDecimal amount;
    /**
     * 手續費金額。
     */
    private BigDecimal feeAmount;
    /**
     * 本次業務總扣款金額。
     */
    private BigDecimal totalDebitAmount;
    /**
     * 交易前餘額。
     */
    private BigDecimal balanceBefore;
    /**
     * 交易後餘額。
     */
    private BigDecimal balanceAfter;
    /**
     * 幣別。
     */
    private String currency;
    /**
     * 交易備註。
     */
    private String note;
    /**
     * 交易建立時間。
     */
    private LocalDateTime createdAt;

    /**
     * 從 TransLog 實體創建 TransLogResponse DTO。
     *
     * @param transLog TransLog 實體。
     * @return 轉換後的 TransLogResponse DTO。
     */
    public static TransLogResponse fromEntity(TransLog transLog) {
        if (transLog == null) {
            return null;
        }
        TransLogResponse response = new TransLogResponse();
        response.setTransactionId(transLog.getTransactionId());
        response.setReferenceId(transLog.getReferenceId());
        response.setAccountNumber(transLog.getAccountNumber());
        response.setCounterpartAccount(transLog.getCounterpartAccount());
        response.setBankCode(transLog.getBankCode());
        response.setBankName(transLog.getBankName());
        response.setCounterpartBankCode(transLog.getCounterpartBankCode());
        response.setCounterpartBankName(transLog.getCounterpartBankName());
        response.setInterbank(transLog.isInterbank());
        response.setEntryType(transLog.getEntryType() != null ? transLog.getEntryType().name() : null);
        response.setTransactionType(transLog.getTransactionType() != null ? transLog.getTransactionType().name() : null);
        response.setAmount(transLog.getAmount());
        response.setFeeAmount(transLog.getFeeAmount());
        response.setTotalDebitAmount(transLog.getTotalDebitAmount());
        response.setBalanceBefore(transLog.getBalanceBefore());
        response.setBalanceAfter(transLog.getBalanceAfter());
        response.setCurrency(transLog.getCurrency() != null ? transLog.getCurrency().name() : null);
        response.setNote(transLog.getNote());
        response.setCreatedAt(transLog.getCreatedAt());
        return response;
    }
}
