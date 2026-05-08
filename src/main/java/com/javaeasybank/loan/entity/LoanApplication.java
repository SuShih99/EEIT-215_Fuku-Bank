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

@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

    @Id
    private String applicationId;           // 申請編號

    private String customerId;              // 用戶ID

    private String applyType;               // 種類
    private BigDecimal applyAmount;         // 金額
    private Integer applyPeriod;            // 期數
    private BigDecimal rate;                // 系統計算利率

    @Enumerated(EnumType.STRING)
    private LoanApplicationStatus applicationStatus;    // 申請狀態

    private LocalDateTime createTime;                   // 申請時間

    @Enumerated(EnumType.STRING)
    private LoanContactStatus latestContactStatus;      // 最新聯絡狀態

    private LocalDateTime latestContactTime;            // 最後聯絡時間

    private LocalDateTime updateTime;                   // 外部模組更新狀態時間
}
