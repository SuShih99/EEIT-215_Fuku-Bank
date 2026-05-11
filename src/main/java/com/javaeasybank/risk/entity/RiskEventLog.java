package com.javaeasybank.risk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RISK_EVENT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RiskEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    //例如: LOAN_SUBMIT, CUSTOMER_CREATE
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    //業務主鍵: 貸款案號或客戶編號
    @Column(name = "business_id", nullable = false, length = 100)
    private String businessId;

    @Column(name = "target_identifier", nullable = false, length = 100)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 50)
    // 儲存 "PASS", "REJECT" 或 "MANUAL_REVIEW"
    private Disposition disposition;

    //描述原因
    @Column(name = "trigger_reason", length = 500)
    private String triggerReason;

    //元參考數據(信用分數 收入
    @Column(name = "meta_data")
    private String metaData;

    @Column(name = "transaction_amount", precision = 18, scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "callback_url", length = 255)
    private String callbackUrl;


    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @CreatedDate
    private LocalDateTime createdAt;

}