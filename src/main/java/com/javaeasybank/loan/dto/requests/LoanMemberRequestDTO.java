package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// ===用戶申請===
@Getter
@Setter
public class LoanMemberRequestDTO {
    private String applyType;
    private BigDecimal applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
}
