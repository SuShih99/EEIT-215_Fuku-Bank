package com.javaeasybank.account.entity;

import com.javaeasybank.account.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 開戶申請實體 — 儲存客戶的開戶申請資料，含 KYC 欄位與風控標記。
 * 狀態流程：PENDING → APPROVED / REJECTED / CANCELLED
 * 審核通過後由 Service 層自動建立 Account。
 */
@Entity
@Table(name = "account_application")
@Getter
@Setter
@NoArgsConstructor
public class AccountApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 關聯 =====

    /** 申請人（已註冊客戶） */
    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    // ===== 帳戶資訊 =====

    /** 申請的帳戶類型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** 幣別（外幣帳戶才需指定，台幣帳戶預設 TWD） */
    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency currency;

    // ===== KYC 個人資料（部分可從 CustomerProfile 帶入） =====

    @Column(name = "customer_name", nullable = false, length = 50)
    private String name;

    @Column(name = "id_number", nullable = false, length = 20)
    private String idNumber;

    @Column(nullable = false)
    private LocalDate birthday;

    /** 國籍（ISO 3166 國碼） */
    @Column(nullable = false, length = 10)
    private String nationality;

    @Column(nullable = false, length = 20)
    private String phone;

    /** 戶籍地址 */
    @Column(name = "registered_address", nullable = false, length = 255)
    private String registeredAddress;

    /** 現居地址 */
    @Column(name = "current_address", nullable = false, length = 255)
    private String currentAddress;

    // ===== 職業與財務背景 =====

    /** 職業別 */
    @Column(length = 50)
    private String occupation;

    /** 任職機構 */
    @Column(name = "employer", length = 100)
    private String employer;

    /** 預估月交易量（萬元） */
    @Column(name = "estimated_monthly_tx")
    private Integer estimatedMonthlyTx;

    /** 開戶目的 */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_purpose", length = 30)
    private AccountPurpose accountPurpose;

    /** 主要資金來源 */
    @Enumerated(EnumType.STRING)
    @Column(name = "fund_source", length = 30)
    private FundSource fundSource;

    // ===== 法遵 =====

    /** 稅務居民國別碼（ISO 3166，CRS 規範） */
    @Column(name = "tax_residency", length = 10)
    private String taxResidency;

    /** PEPs 聲明：本人或家庭成員是否為重要政治性職務之人 */
    @Column(name = "is_pep", nullable = false)
    private Boolean isPep = false;

    // ===== 證件圖片 =====

    /** 身分證正面圖片路徑 */
    @Column(name = "id_front_url", nullable = false, length = 255)
    private String idFrontUrl;

    /** 身分證反面圖片路徑 */
    @Column(name = "id_back_url", nullable = false, length = 255)
    private String idBackUrl;

    /** 第二證件圖片路徑（健保卡 / 駕照） */
    @Column(name = "second_id_url", nullable = false, length = 255)
    private String secondIdUrl;

    // ===== 風控 =====

    /** 風險標記 */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_flag", nullable = false, length = 30)
    private RiskFlag riskFlag = RiskFlag.NORMAL;

    /** 申請時的 IP 位址 */
    @Column(name = "apply_ip", length = 45)
    private String applyIp;

    // ===== 審核 =====

    /** 申請狀態 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /** 駁回原因 */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 審核時間 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 審核人員 */
    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    /** 審核通過後建立的帳號 */
    @Column(name = "created_account_number", length = 12)
    private String createdAccountNumber;

    // ===== 時間戳 =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
