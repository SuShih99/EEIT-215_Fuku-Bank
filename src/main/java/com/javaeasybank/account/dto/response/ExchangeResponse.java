package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.enums.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExchangeResponse {

    private String referenceId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private Currency fromCurrency;
    private Currency toCurrency;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal exchangeRate;
    private BigDecimal fromAccountBalance;
    private BigDecimal toAccountBalance;
    private LocalDateTime exchangedAt;
}
