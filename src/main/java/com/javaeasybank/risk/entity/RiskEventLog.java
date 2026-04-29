package com.javaeasybank.risk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "[RiskEventLog]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[LogId]")
    private Long logId;

    @Column(name = "[EventType]", nullable = false, length = 50)
    private String eventType;

    @Column(name = "[TargetIdentifier]", nullable = false, length = 100)
    private String targetIdentifier;

    @Column(name = "[RiskLevel]", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "[ActionTaken]", nullable = false, length = 50)
    private String actionTaken;

    @Column(name = "[TriggerReason]", length = 500)
    private String triggerReason;

    @Column(name = "[TransactionAmount]", precision = 18, scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "[CreatedAt]", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 確保在寫入資料庫前，自動填上當前時間
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
