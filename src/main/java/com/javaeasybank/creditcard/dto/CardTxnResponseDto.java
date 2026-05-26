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
    private String customerName;
    private String merchantName;
    private String cardNumber;
    private Integer refTxnId;
    //此欄位純設計給前台看
    private Boolean refunded;

    private BigDecimal cashbackRate;
    private BigDecimal cashbackAmount;
}
