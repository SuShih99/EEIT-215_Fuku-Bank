package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.javaeasybank.creditcard.enums.TxnType;

import lombok.Data;

@Data
public class CardTxnResponseDto {

    private Integer txnId;
    private BigDecimal txnAmount;
    private TxnType txnType;
    private LocalDateTime txnDate;
    private String description;

    private String merchantName;
    private String cardNumber;
}
