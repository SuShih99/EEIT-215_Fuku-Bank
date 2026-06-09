package com.javaeasybank.loan.entity;

/**
 * 程式說明：
 * - 貸款申請主檔 Entity，對應資料表 LOAN_APPLICATION。
 * - 記錄會員申請內容、聯絡進度、文件補件、審核狀態與申請生命週期時間。
 */

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 貸款申請主表 Entity，對應資料庫 LOAN_APPLICATION
/**
 * 貸款申請主表 Entity，對應資料庫 LOAN_APPLICATION。
 * 記錄客戶送件內容、目前審核狀態、最新聯繫摘要與補件批次資訊。
 */
@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication implements Persistable<String> {

    /** 讓 Spring Data JPA 的 save() 正確走 persist() 而非 merge() */
    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return applicationId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }

    // 申請唯一識別碼（UUID），作為主鍵
    @Id
    private String applicationId;

    // 客戶內部識別碼（系統內部使用，不對外顯示）
    private String customerId;

    // ── 申請條件 ─────────────────────────────────────────────────────

    // 貸款種類，例如 "PERSONAL"（信貸）、"HOUSE"（房貸）
    private String applyType;

    // 客戶申請金額（新台幣）
    private BigDecimal applyAmount;

    // 客戶申請期數（月）
    private Integer applyPeriod;

    // 申請時顯示的年利率（百分比小數）
    private BigDecimal rate;

    // 客戶選擇的撥款入帳帳號（台幣活存），核准後撥款使用
    private String disbursementAccount;

    // ── 狀態 ─────────────────────────────────────────────────────────

    // 申請目前的狀態，以字串形式存入 DB，參見 LoanApplicationStatus
    @Enumerated(EnumType.STRING)
    private LoanApplicationStatus applicationStatus;

    @Lob
    @Column(name = "required_documents", columnDefinition = "NVARCHAR(MAX)")
    private String requiredDocuments;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String reviewComment;

    // 申請建立時間
    private LocalDateTime createTime;

    // ── 聯繫紀錄快照（從 LoanContactLog 更新而來）────────────────────

    // 最近一筆聯繫紀錄的結果狀態快照
    @Enumerated(EnumType.STRING)
    private LoanContactStatus latestContactStatus;

    // 最近一筆聯繫紀錄的時間快照
    private LocalDateTime latestContactTime;

    // 外部模組（風控 / 帳戶）回調後更新狀態的時間
    private LocalDateTime updateTime;

    // 客戶送出補件的時間戳記
    private LocalDateTime documentsSubmittedAt;

    // 目前補件批次；0 代表初始申請文件，1 起代表第 N 次退回補件
    private Integer currentSupplementBatchNo = 0;
}
