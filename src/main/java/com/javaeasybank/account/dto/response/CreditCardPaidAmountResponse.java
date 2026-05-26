package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditCardPaidAmountResponse {

    private String creditCardAccountNumber;
    private BigDecimal paidAmount;
    private BigDecimal currentCreditCardBalance;
    private String billingMonth;
    private String startDate;
    private String endDate;
}
