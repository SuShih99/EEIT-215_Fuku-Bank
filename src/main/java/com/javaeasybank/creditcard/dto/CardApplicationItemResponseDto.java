package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

public record CardApplicationItemResponseDto
	(Integer itemId,
    String cardTypeName,
    String result,
    BigDecimal approvedLimit,
    BigDecimal annualFee,
    Boolean createCardFlag,
    String remark) {
}
