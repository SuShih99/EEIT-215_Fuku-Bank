package com.javaeasybank.creditcard.dto;

public record CardSummaryDto(
    Integer cardId,
    String cardNumber,
    String cardTypeName
) {

}
