package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

import lombok.Data;
@Data
public class CardTypeRequestDto {

	private String cardTypeName;
    private String brand;
    private BigDecimal annualFee;
    private BigDecimal cashbackRate;
    private String cardImageUrl;
}
