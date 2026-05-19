package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanDocumentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 補件文件查詢回應 DTO。
 *
 * <p>回傳單筆客戶上傳文件的基本資訊，
 * 對應 {@code GET /api/staff/loan-applications/{id}/documents} 的列表項目。</p>
 *
 * <p>注意：文件的審核狀態由風控模組負責維護，
 * 貸款模組僅記錄上傳資訊，不追蹤審核結果。</p>
 */
@Getter
@Setter
public class LoanDocumentResponseDTO {

    /** 文件唯一識別碼（UUID）。 */
    private String documentId;

    /** 關聯的貸款申請識別碼。 */
    private String applicationId;

    /** 文件類型，參見 {@code LoanDocumentType}。 */
    private LoanDocumentType documentType;

    /** 文件儲存的存取 URL，前端可透過此連結預覽或下載。 */
    private String fileUrl;

    /** 客戶上傳時的原始檔名，例如 {@code "薪資證明_2024.pdf"}。 */
    private String originalName;

    /** 上傳者識別碼（通常為客戶的 customerId 或 CIF）。 */
    private String uploadedBy;

    /** 文件上傳時間。 */
    private LocalDateTime uploadTime;
}
