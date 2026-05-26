package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanAccountTransactionResponse {

    private String referenceId;
    private String loanAccountNumber;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal remainingLiability;
    private BigDecimal sourceAccountBalance;
    private BigDecimal targetAccountBalance;
    private LocalDateTime transactedAt;
}
