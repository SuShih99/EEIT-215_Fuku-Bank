package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanReviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_REVIEW_DETAIL")
@Getter
@Setter
@NoArgsConstructor
public class LoanReviewDetail implements Persistable<String> {

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return reviewId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }

    // 審核詳情唯一識別碼（UUID），作為主鍵
    @Id
    private String reviewId;

    // 關聯的貸款申請識別碼（一對一）
    private String applicationId;

    // 行員確認的核准金額（新台幣）
    @Column(precision = 18, scale = 2)
    private BigDecimal confirmedAmount;

    // 行員確認的核准期數（月）
    private Integer confirmedPeriod;

    // 行員確認的核准年利率（百分比小數）
    @Column(precision = 10, scale = 6)
    private BigDecimal confirmedRate;

    // 擔保品備註說明，記錄擔保品種類或特殊條件（選填）
    private String collateralNote;

    // 填寫此份審核詳情的行員工號
    private String empId;

    // 最後儲存（草稿或送審）的時間戳記，每次覆寫時更新
    private LocalDateTime reviewTime;

    // 審核詳情的填寫狀態，以字串形式存入 DB
    @Enumerated(EnumType.STRING)
    private LoanReviewStatus reviewStatus;

    // 行員正式送審的時間戳記
    private LocalDateTime submittedTime;

    // 審核備註，例如核准條件調整的原因說明（選填）
    private String reviewNote;
}
