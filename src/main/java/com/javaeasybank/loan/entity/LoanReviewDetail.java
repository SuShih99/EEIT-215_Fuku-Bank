package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanReviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行員二次填單（審核詳情）Entity，對應資料庫 {@code LOAN_REVIEW_DETAIL}。
 *
 * <p>行員在聯繫客戶確認意願後，填寫核准條件（金額、期數、利率等）的調整結果。
 * 每筆申請最多對應一筆審核詳情（一對一），行員可覆寫草稿多次，
 * 確認後再送審（{@code SUBMITTED}）觸發風控審核流程。</p>
 */
@Entity
@Table(name = "LOAN_REVIEW_DETAIL")
@Getter
@Setter
@NoArgsConstructor
public class LoanReviewDetail {

    /** 審核詳情唯一識別碼（UUID），作為主鍵。 */
    @Id
    private String reviewId;

    /** 關聯的貸款申請識別碼（一對一）。 */
    private String applicationId;

    // ── 行員確認的核准條件 ────────────────────────────────────────────

    /**
     * 行員確認的核准金額（新台幣）。
     * 精確度 18 位，小數 2 位，以容納大額貸款計算。
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal confirmedAmount;

    /** 行員確認的核准期數（月）。 */
    private Integer confirmedPeriod;

    /**
     * 行員確認的核准年利率（百分比小數）。
     * 精確度 10 位，小數 6 位，以容納細緻的利率計算。
     */
    @Column(precision = 10, scale = 6)
    private BigDecimal confirmedRate;

    /** 擔保品備註說明，記錄擔保品種類或特殊條件（選填）。 */
    private String collateralNote;

    // ── 行員資訊 ─────────────────────────────────────────────────────

    /** 填寫此份審核詳情的行員工號。 */
    private String empId;

    /** 最後儲存（草稿或送審）的時間戳記，每次覆寫時更新。 */
    private LocalDateTime reviewTime;

    /**
     * 審核詳情的填寫狀態，以字串形式存入 DB。
     * 參見 {@code LoanReviewStatus}（{@code DRAFT} / {@code SUBMITTED}）。
     */
    @Enumerated(EnumType.STRING)
    private LoanReviewStatus reviewStatus;

    /**
     * 行員正式送審的時間戳記。
     * 狀態為 {@code DRAFT} 時此欄位為 {@code null}；
     * 送審後由 {@code LoanApplicationService} 寫入。
     */
    private LocalDateTime submittedTime;

    /** 審核備註，例如核准條件調整的原因說明（選填）。 */
    private String reviewNote;
}
