package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditCardSettlementResponse {

    private String referenceId;
    private String creditCardAccountNumber;
    private BigDecimal amount;
    private BigDecimal creditCardBalance;
    private BigDecimal bankCollectionBalance;
    private LocalDateTime transactedAt;
}
