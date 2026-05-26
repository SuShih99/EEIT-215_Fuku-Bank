package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditCardPaymentResponse {

    private String referenceId;
    private String creditCardAccountNumber;
    private String fromAccountNumber;
    private BigDecimal amount;
    private BigDecimal fromAccountBalance;
    private BigDecimal creditCardBalance;
    private LocalDateTime transactedAt;
}
