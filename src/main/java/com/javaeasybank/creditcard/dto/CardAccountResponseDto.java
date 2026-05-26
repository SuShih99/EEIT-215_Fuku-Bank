package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CardAccountResponseDto {

    private Integer id;
    private String accountNumber;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private BigDecimal availableCredit;
    private Integer statementDay;
    private Integer dueDays;
    private String customerId;
    private String customerName;
    private List<CardSummaryDto> cards;

}
