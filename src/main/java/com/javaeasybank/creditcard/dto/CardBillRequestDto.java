package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.javaeasybank.creditcard.enums.BillStatus;

import lombok.Data;

@Data
public class CardBillRequestDto {

    private Integer cardId;
    private String billingMonth;
    private LocalDate billDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal minimumPayment;
    private BigDecimal paidAmount;
    private BillStatus billStatus;
    private Integer refTxnId;

    private List<CardTxnRequestDto> transactions;
}
