package com.javaeasybank.customer.loan.dto.response;

import com.javaeasybank.customer.loan.enums.LoanReviewStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class LoanReviewDetailResponseDTO {
    private String reviewId;
    private String applicationId;
    private Long confirmedAmount;
    private Integer confirmedPeriod;
    private BigDecimal confirmedRate;
    private String collateralNote;
    private String empId;
    private LocalDateTime reviewTime;
    private LoanReviewStatus reviewStatus;
    private LocalDateTime submittedTime;
    private String reviewNote;
}
