package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// ===非會員申請===
@Getter
@Setter
public class LoanNonMemberRequestDTO {
    private String applicantName;
    private String applicantPhone;
    private String applicantEmail;
    private String applyType;
    private Long applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
}
