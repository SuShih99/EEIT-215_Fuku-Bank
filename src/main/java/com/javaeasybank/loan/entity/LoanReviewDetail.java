package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanReviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_REVIEW_DETAIL")
@Getter
@Setter
@NoArgsConstructor
public class LoanReviewDetail {

    @Id
    private String reviewId;               // review_id (PK)

    private String applicationId;          // application_id (FK)

    private BigDecimal confirmedAmount;
    private Integer confirmedPeriod;
    private BigDecimal confirmedRate;
    private String collateralNote;         // 擔保品資料

    private String empId;
    private LocalDateTime reviewTime;

    @Enumerated(EnumType.STRING)
    private LoanReviewStatus reviewStatus;

    private LocalDateTime submittedTime;
    private String reviewNote;
}
