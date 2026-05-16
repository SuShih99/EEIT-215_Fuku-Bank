package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_DOCUMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanDocument {

    @Id
    private String documentId;          // 文件編號

    private String applicationId;       // 關聯申請編號

    @Enumerated(EnumType.STRING)
    private LoanDocumentType documentType;  // 文件類型

    private String fileUrl;             // 實體檔案路徑（FileStorageService 回傳）
    @Column(columnDefinition = "NVARCHAR(255)")
    private String originalName;        // 原始檔名（前端顯示用）

    private String uploadedBy;          // 上傳者 customerId
    private LocalDateTime uploadTime;   // 上傳時間
    // 審核由風控負責，貸款模組僅負責儲存與傳遞至風控
}
