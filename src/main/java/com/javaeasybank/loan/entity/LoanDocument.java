package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 貸款補件文件 Entity，對應資料庫 LOAN_DOCUMENT。
 * 保存客戶上傳文件的批次、類型、檔案路徑與送審狀態，用於風控退件補件流程。
 */
@Entity
@Table(name = "LOAN_DOCUMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanDocument {

    // 文件唯一識別碼（UUID），作為主鍵
    @Id
    private String documentId;

    // 關聯的貸款申請識別碼
    private String applicationId;

    // 文件類型，以字串形式存入 DB
    @Enumerated(EnumType.STRING)
    private LoanDocumentType documentType;

    // 實體檔案的存取 URL，由 FileStorageService 儲存後回傳
    private String fileUrl;

    // 客戶上傳時的原始檔名，例如 "薪資證明_2024.pdf"
    @Column(columnDefinition = "NVARCHAR(255)")
    private String originalName;

    // 上傳者識別碼（客戶的 customerId）
    private String uploadedBy;

    // 文件上傳時間，由 LoanDocumentService 寫入當下時間
    private LocalDateTime uploadTime;

    // INITIAL = 聯繫階段申請文件；SUPPLEMENT = 退回補件文件
    private String documentBatchType;

    // INITIAL 固定 0；SUPPLEMENT 由 LoanApplication.currentSupplementBatchNo 決定
    private Integer documentBatchNo;

    // 本批次文件送出時間；null = 草稿可編輯
    private LocalDateTime submittedAt;

    @PrePersist
    private void applyBatchDefaults() {
        if (documentBatchType == null) {
            documentBatchType = "INITIAL";
        }
        if (documentBatchNo == null) {
            documentBatchNo = 0;
        }
    }
}
