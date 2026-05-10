package com.javaeasybank.customer.loan.dto.response;

import com.javaeasybank.customer.loan.enums.LoanApplicationStatus;
import com.javaeasybank.customer.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class LoanApplicationResponseDTO {
    private String applicationId;
    private String customerId;
    private String applicantName;
    private String applicantPhone;
    private String applicantEmail;
    private String applyType;
    private Long applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
    private LoanApplicationStatus applicationStatus;
    private LocalDateTime createTime;
    private LoanContactStatus latestContactStatus;
    private LocalDateTime latestContactTime;
}
