package com.javaeasybank.risk.entity;

import com.javaeasybank.account.enums.FundSource;
import com.javaeasybank.risk.enums.Occupation;
import com.javaeasybank.risk.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_credit_info")
@Getter
@Setter
public class CustomerCreditInfo {
    // 這裡的 ID 型態與長度必須跟 CustomerProfile 一致
    @Id
    @Column(name = "customer_id", length = 20)
    private String customerId;

    // --- 財務維度 (模擬外部輸入) ---
    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation", length = 50, columnDefinition = "NVARCHAR(50)")
    // 職業
    private Occupation occupation;
    //風控
    @Nationalized
    @Column(name = "job", length = 100, columnDefinition = "NVARCHAR(100)")
    private String  job;

    // --- 信用維度 (模擬聯徵/外部資料) ---
    @Column(name = "external_score")
    private Integer externalScore = 0; // 300 ~ 800

    @Column(name = "other_bank_debt", precision = 15, scale = 2)
    private BigDecimal otherBankDebt = BigDecimal.ZERO;

    @Column(name = "has_real_estate")
    private Boolean hasRealEstate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fund_source", length = 30)
    private FundSource fundSource;

    @Column(name = "is_pep", columnDefinition = "BIT NOT NULL DEFAULT 0")
    private Boolean isPep = false;

    // --- 評分結果 (Service計算後回填) ---


    @Column(name = "final_score")
    private Integer finalScore; // 0 ~ 100

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel; // LOW, MEDIUM, HIGH

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
