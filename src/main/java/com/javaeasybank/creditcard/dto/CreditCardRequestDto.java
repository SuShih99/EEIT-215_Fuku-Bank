package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreditCardRequestDto {

    private String customerId;
    private Integer cardTypeId;
    private Integer applicationItemId;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private String cardImageUrl;

}
