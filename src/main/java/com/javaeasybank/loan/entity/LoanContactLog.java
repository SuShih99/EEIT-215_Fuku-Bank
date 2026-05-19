package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 行員聯繫紀錄 Entity，對應資料庫 {@code LOAN_CONTACT_LOG}。
 *
 * <p>採 append-only 設計：行員每次聯繫後新增一筆紀錄，不修改歷史資料。
 * 新增後由 {@code LoanApplicationService} 同步更新
 * {@code LoanApplication.latestContactStatus} 與 {@code latestContactTime}。</p>
 */
@Entity
@Table(name = "LOAN_CONTACT_LOG")
@Getter
@Setter
@NoArgsConstructor
public class LoanContactLog {

    /** 聯繫紀錄唯一識別碼（UUID），作為主鍵。 */
    @Id
    private String logId;

    /** 關聯的貸款申請識別碼。 */
    private String applicationId;

    /** 執行聯繫的行員工號。 */
    private String empId;

    /**
     * 此次聯繫的結果狀態，以字串形式存入 DB。
     * 參見 {@code LoanContactStatus}。
     */
    @Enumerated(EnumType.STRING)
    private LoanContactStatus contactStatus;

    /**
     * 此次聯繫使用的溝通管道，以字串形式存入 DB。
     * 參見 {@code LoanContactChannel}。
     */
    @Enumerated(EnumType.STRING)
    private LoanContactChannel contactChannel;

    /** 聯繫發生的時間戳記，由 Service 層在建立時寫入當下時間。 */
    private LocalDateTime contactTime;

    /** 行員備註說明，記錄聯繫過程中的特殊情況或客戶反饋（選填）。 */
    private String note;
}
