package com.javaeasybank.loan.entity;

/**
 * 程式說明：
 * - 貸款聯絡紀錄 Entity，對應資料表 LOAN_CONTACT_LOG。
 * - 保存每次與申請人聯絡的時間、管道、結果、處理員工與備註。
 */

import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * 貸款聯繫紀錄 Entity，對應資料庫 LOAN_CONTACT_LOG。
 * 記錄行員與客戶的電話、Email 或簡訊互動，並作為案件最新聯繫狀態的來源。
 */
@Entity
@Table(name = "LOAN_CONTACT_LOG")
@Getter
@Setter
@NoArgsConstructor
public class LoanContactLog implements Persistable<String> {

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return logId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }

    // 聯繫紀錄唯一識別碼（UUID），作為主鍵
    @Id
    private String logId;

    // 關聯的貸款申請識別碼
    private String applicationId;

    // 執行聯繫的行員工號
    private String empId;

    // 此次聯繫的結果狀態，以字串形式存入 DB
    @Enumerated(EnumType.STRING)
    private LoanContactStatus contactStatus;

    // 此次聯繫使用的溝通管道，以字串形式存入 DB
    @Enumerated(EnumType.STRING)
    private LoanContactChannel contactChannel;

    // 聯繫發生的時間戳記，由 Service 層在建立時寫入當下時間
    private LocalDateTime contactTime;

    // 行員備註說明，記錄聯繫過程中的特殊情況或客戶反饋（選填）
    private String note;
}
