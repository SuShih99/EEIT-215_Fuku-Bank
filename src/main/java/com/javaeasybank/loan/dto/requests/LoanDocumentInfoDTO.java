package com.javaeasybank.loan.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 傳送給風控模組的補件文件資訊 DTO。
 *
 * <p>送審時由 {@code LoanRiskClient} 組裝進 {@code LoanRiskRequestDTO}，
 * 將該筆申請所有已上傳的文件清單一併傳遞給風控系統進行審核。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocumentInfoDTO {

    /** 文件唯一識別碼，對應 {@code LoanDocument.documentId}。 */
    private String documentId;

    /**
     * 文件類型字串，為 {@code LoanDocumentType} 列舉的 {@code name()} 值。
     * 例如：{@code "ID_CARD"}、{@code "INCOME_CERT"}。
     */
    private String documentType;

    /** 文件檔案的存取 URL，供風控系統下載或預覽使用。 */
    private String fileUrl;

    /** 客戶上傳時的原始檔名，例如 {@code "薪資證明.pdf"}。 */
    private String originalName;
}
