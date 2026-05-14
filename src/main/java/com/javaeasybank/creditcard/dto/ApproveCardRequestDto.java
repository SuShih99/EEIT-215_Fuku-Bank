package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

public record ApproveCardRequestDto(
    BigDecimal approvedLimit) {
}
