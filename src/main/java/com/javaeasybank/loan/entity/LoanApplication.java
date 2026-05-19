package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 貸款申請主表 Entity，對應資料庫 {@code LOAN_APPLICATION}。
 *
 * <p>記錄客戶從提交申請到結案的完整流程資訊，包含申請條件、
 * 申請狀態、最新聯繫紀錄快照及補件狀態。
 * 狀態流轉由 {@code LoanApplicationService} 統一管理。</p>
 */
@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

    /** 申請唯一識別碼（UUID），作為主鍵。 */
    @Id
    private String applicationId;

    /** 客戶內部識別碼（系統內部使用，不對外顯示）。 */
    private String customerId;

    // ── 申請條件 ─────────────────────────────────────────────────────

    /** 貸款種類，例如 {@code "PERSONAL"}（信貸）、{@code "HOUSE"}（房貸）。 */
    private String applyType;

    /** 客戶申請金額（新台幣）。 */
    private BigDecimal applyAmount;

    /** 客戶申請期數（月）。 */
    private Integer applyPeriod;

    /** 申請時顯示的年利率（百分比小數）。 */
    private BigDecimal rate;

    /** 客戶選擇的撥款入帳帳號（台幣活存），核准後撥款使用。 */
    private String disbursementAccount;

    // ── 狀態 ─────────────────────────────────────────────────────────

    /** 申請目前的狀態，以字串形式存入 DB，參見 {@code LoanApplicationStatus}。 */
    @Enumerated(EnumType.STRING)
    private LoanApplicationStatus applicationStatus;

    /** 申請建立時間。 */
    private LocalDateTime createTime;

    // ── 聯繫紀錄快照（從 LoanContactLog 更新而來）────────────────────

    /**
     * 最近一筆聯繫紀錄的結果狀態快照。
     * 每次新增 {@code LoanContactLog} 後由 {@code LoanApplicationService} 同步更新。
     */
    @Enumerated(EnumType.STRING)
    private LoanContactStatus latestContactStatus;

    /** 最近一筆聯繫紀錄的時間快照。 */
    private LocalDateTime latestContactTime;

    /**
     * 外部模組（風控 / 帳戶）回調後更新狀態的時間。
     * 行員後台可依此欄位排序，將最近有異動的申請置頂顯示。
     */
    private LocalDateTime updateTime;

    /**
     * 客戶送出補件的時間戳記。
     * {@code null} 表示客戶尚未送出補件；送出後由 {@code LoanDocumentService} 寫入。
     */
    private LocalDateTime documentsSubmittedAt;
}
