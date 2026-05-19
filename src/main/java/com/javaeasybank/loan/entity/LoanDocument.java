package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 補件文件 Entity，對應資料庫 {@code LOAN_DOCUMENT}。
 *
 * <p>儲存客戶於補件階段上傳的文件後設資訊（metadata）；
 * 實體檔案由 {@code FileStorageService} 儲存，此 Entity 僅保存存取 URL 與上傳資訊。</p>
 *
 * <p><b>職責分工：</b>貸款模組負責儲存與傳遞文件至風控系統，
 * 文件的審核狀態由風控模組維護，此 Entity 不追蹤審核結果。</p>
 */
@Entity
@Table(name = "LOAN_DOCUMENT")
@Getter
@Setter
@NoArgsConstructor
public class LoanDocument {

    /** 文件唯一識別碼（UUID），作為主鍵。 */
    @Id
    private String documentId;

    /** 關聯的貸款申請識別碼。 */
    private String applicationId;

    /**
     * 文件類型，以字串形式存入 DB。
     * 參見 {@code LoanDocumentType}。
     */
    @Enumerated(EnumType.STRING)
    private LoanDocumentType documentType;

    /** 實體檔案的存取 URL，由 {@code FileStorageService} 儲存後回傳。 */
    private String fileUrl;

    /**
     * 客戶上傳時的原始檔名，例如 {@code "薪資證明_2024.pdf"}。
     * 使用 {@code NVARCHAR(255)} 以支援中文等多位元組字元。
     */
    @Column(columnDefinition = "NVARCHAR(255)")
    private String originalName;

    /** 上傳者識別碼（客戶的 {@code customerId}）。 */
    private String uploadedBy;

    /** 文件上傳時間，由 {@code LoanDocumentService} 寫入當下時間。 */
    private LocalDateTime uploadTime;
}
