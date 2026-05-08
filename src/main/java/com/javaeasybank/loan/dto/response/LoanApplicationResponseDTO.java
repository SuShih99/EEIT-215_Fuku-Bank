package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class LoanApplicationResponseDTO {
    private String applicationId;
    private String customerId;
    private String applyType;
    private BigDecimal applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
    private LoanApplicationStatus applicationStatus;
    private LocalDateTime createTime;
    private LoanContactStatus latestContactStatus;
    private LocalDateTime latestContactTime;
}
