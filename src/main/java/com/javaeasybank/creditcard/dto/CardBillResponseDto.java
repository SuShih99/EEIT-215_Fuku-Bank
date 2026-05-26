package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.javaeasybank.creditcard.enums.BillStatus;

import lombok.Data;

@Data
public class CardBillResponseDto {

	private Integer billId;

    private String customerName;

    private String billingMonth;
    private LocalDate billDate;
    private LocalDate dueDate;

    private BigDecimal totalAmount;
    private BigDecimal minimumPayment;
    private BigDecimal paidAmount;

    private BillStatus billStatus;

    private String creditCardAccountNumber;

    private BigDecimal availableCredit;

    private String accountNumber;
    private BigDecimal creditLimit;

    private BigDecimal cashbackAmount;

    private Boolean rewardPosted;

    private String rewardReferenceId;

    private List<CardSummaryDto> cards;
}
