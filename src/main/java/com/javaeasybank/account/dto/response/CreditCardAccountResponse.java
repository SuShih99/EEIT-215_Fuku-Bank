package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditCardAccountResponse {

    private String creditCardAccountNumber;
    private String customerId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
