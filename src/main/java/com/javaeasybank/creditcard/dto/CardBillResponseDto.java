package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.javaeasybank.creditcard.enums.BillStatus;

import lombok.Data;

@Data
public class CardBillResponseDto {

	private Integer billId;
    private String billingMonth;
    private LocalDate billDate;
    private LocalDate dueDate;

    private BigDecimal totalAmount;
    private BigDecimal minimumPayment;
    private BigDecimal paidAmount;

    private BillStatus billStatus;
}
