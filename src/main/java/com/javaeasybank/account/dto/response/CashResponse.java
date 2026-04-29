package com.javaeasybank.account.dto.response;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CashResponse {

    // 交易編號
    private String referenceId;

    // 帳號
    private String accountNumber;

    // 交易金額
    private BigDecimal amount;

    // 餘額
    private BigDecimal balance;

    // 交易時間
    private LocalDateTime transactedAt;
}
