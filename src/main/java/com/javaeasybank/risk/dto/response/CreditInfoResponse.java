package com.javaeasybank.risk.dto.response;

import com.javaeasybank.account.enums.FundSource;
import com.javaeasybank.risk.enums.Occupation;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreditInfoResponse {
    private String customerId;
    private String customerName;
    private BigDecimal annualIncome;
    private Occupation occupation;
    private String job;
    private Integer externalScore;
    private BigDecimal otherBankDebt;
    private Boolean hasRealEstate;
    private FundSource fundSource;
    private Boolean isPep;
    private Integer finalScore;
    private RiskLevel riskLevel;
    private LocalDateTime lastUpdatedAt;
}
