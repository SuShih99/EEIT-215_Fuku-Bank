package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.javaeasybank.creditcard.enums.CardStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardResponseDto {

	private Integer cardId;
    private String cardNumber;
    private String customerName;
    private CardTypeResponseDto cardType;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private CardStatus status;
    private LocalDate expiryDate;
}
