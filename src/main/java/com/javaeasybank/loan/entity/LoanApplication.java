package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

    @Id
    // 申請ID
    private String applicationId;

    // 用戶ID
    private String customerId;

    // 申請種類/金額/期數/利率/入款帳戶(台幣活存)
    private String applyType;
    private BigDecimal applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
    private String disbursementAccount;

    // 申請狀態
    @Enumerated(EnumType.STRING)
    private LoanApplicationStatus applicationStatus;

    @Lob
    @Column(name = "required_documents", columnDefinition = "NVARCHAR(MAX)")
    private String requiredDocuments;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String reviewComment;

    // 申請時間
    private LocalDateTime createTime;

    // 最新聯絡狀態
    @Enumerated(EnumType.STRING)
    private LoanContactStatus latestContactStatus;

    // 最後聯絡時間
    private LocalDateTime latestContactTime;

    // 外部模組更新狀態時間
    private LocalDateTime updateTime;

    // 客戶送出補件的時間
    private LocalDateTime documentsSubmittedAt;
}
