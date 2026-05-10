package com.javaeasybank.customer.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// ===儲存二次填單草稿===
@Getter
@Setter
public class LoanReviewDetailRequestDTO {
    private Long confirmedAmount;
    private Integer confirmedPeriod;
    private BigDecimal confirmedRate;
    private String collateralNote;
    private String empId;
}
