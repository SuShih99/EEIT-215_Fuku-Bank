package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

import com.javaeasybank.creditcard.enums.TxnType;

import lombok.Data;

@Data
public class CardTxnRequestDto {

	private BigDecimal txnAmount;
    private TxnType txnType;
    private String description;
    private Integer cardId;
    private Integer merchantId;
}
