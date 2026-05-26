package com.javaeasybank.creditcard.dto;

import lombok.Data;

@Data
public class CreditCardRequestDto {

    private String customerId;
    private Integer cardTypeId;
    private Integer applicationItemId;
    private String cardImageUrl;

}
