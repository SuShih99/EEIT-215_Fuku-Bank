package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CardBillDetailDto {

	private Integer billId;
    private String billingMonth;
    private BigDecimal totalAmount;

    private List<CardTxnResponseDto> transactions;
}
