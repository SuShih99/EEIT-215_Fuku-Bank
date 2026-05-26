package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanAccountResponse {

    private String loanAccountNumber;
    private String customerId;
    private BigDecimal liability;
    private BigDecimal rate;
    private LocalDateTime createdAt;
}
