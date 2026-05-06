package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

public record CardApplicationItemRequestDto(
    Integer itemId,
    Integer applicationId,
    Integer cardTypeId,
    String result,
    BigDecimal approvedLimit,
    BigDecimal annualFee,
    Boolean createCardFlag,
    String remark
    

) {
}