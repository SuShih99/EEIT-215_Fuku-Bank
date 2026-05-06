package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CardApplicationItemResponseDto
	(Integer itemId,
    String cardTypeName,
    String result,
    BigDecimal approvedLimit,
    BigDecimal annualFee,
    Boolean createCardFlag,
    LocalDateTime reviewDate,
    String remark) {
}
