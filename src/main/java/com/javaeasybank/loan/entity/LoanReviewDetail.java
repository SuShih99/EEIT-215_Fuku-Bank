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
    // 填單ID
    private String reviewId;

    // 申請ID
    private String applicationId;

    // 確認金額/期數/利率
    @Column(precision = 18, scale = 2)
    private BigDecimal confirmedAmount;
    private Integer confirmedPeriod;
    @Column(precision = 10, scale = 6)
    private BigDecimal confirmedRate;

    // 擔保品備註
    private String collateralNote;

    // 填單人員ID
    private String empId;

    // 填單時間
    private LocalDateTime reviewTime;

    // 填單狀態
    @Enumerated(EnumType.STRING)
    private LoanReviewStatus reviewStatus;

    // 送審時間
    private LocalDateTime submittedTime;

    // 送審備註
    private String reviewNote;
}
