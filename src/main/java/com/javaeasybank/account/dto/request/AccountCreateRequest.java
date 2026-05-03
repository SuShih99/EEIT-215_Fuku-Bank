package com.javaeasybank.account.dto.request;

import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 帳戶創建請求 DTO，用於接收客戶端的帳戶創建資訊。
 */
@Data
public class AccountCreateRequest {
    /**
     * 客戶 ID，不可為空。
     */
    @NotNull
    private String customerId;
    
    /**
     * 帳戶類型，不可為空。
     */
    @NotNull
    private AccountType accountType;
    
    /**
     * 帳戶貨幣，不可為空。
     */
    @NotNull
    private Currency currency;

    /**
     * 父帳號 (子帳戶創建時需要提供)。
     */
    private String parentAccountNumber;
}
