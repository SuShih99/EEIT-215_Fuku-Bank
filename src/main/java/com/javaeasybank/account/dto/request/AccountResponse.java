package com.javaeasybank.account.dto.request;

import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帳戶響應 DTO，用於向客戶端返回帳戶資訊。
 */
@Data
public class AccountResponse {
    /**
     * 帳號。
     */
    private String accountNumber;
    /**
     * 客戶 ID。
     */
    private String customerId;
    /**
     * 客戶姓名。
     */
    private String customerName;
    /**
     * 帳戶類型。
     */
    private AccountType accountType;
    /**
     * 帳戶貨幣。
     */
    private Currency currency;
    /**
     * 帳戶餘額。
     */
    private BigDecimal balance;
    /**
     * 帳戶負債。
     */
    private BigDecimal liability;
    /**
     * 帳戶利率。
     */
    private BigDecimal interestRate;
    /**
     * 帳戶狀態。
     */
    private AccountStatus status;
    /**
     * 父帳號 (子帳戶專用)。
     */
    private String parentAccountNumber;
    /**
     * 帳戶創建時間。
     */
    private LocalDateTime createdAt;
    
    /**
     * 從 Account 實體創建 AccountResponse DTO。
     *
     * @param account Account 實體。
     * @return 轉換後的 AccountResponse DTO。
     */


    public static AccountResponse fromEntity(Account account) {
        return fromEntity(account, null);
    }

    public static AccountResponse fromEntity(Account account, String customerName) {
        AccountResponse response = new AccountResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setCustomerId(account.getCustomerId());
        response.setCustomerName(customerName);
        response.setAccountType(account.getAccountType());
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());
        response.setLiability(account.getLiability());
        response.setInterestRate(account.getInterestRate());
        response.setStatus(account.getStatus());
        response.setParentAccountNumber(account.getParentAccountNumber());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }
}
