package com.javaeasybank.loan.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 補件文件摘要 DTO。
 * 送風控時描述單一文件的類型、檔名與檔案位置。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocumentInfoDTO {

    // 文件唯一識別碼，對應 LoanDocument.documentId
    private String documentId;

    // 文件類型字串，為 LoanDocumentType 列舉的 name() 值
    private String documentType;

    // 文件檔案的存取 URL，供風控系統下載或預覽使用
    private String fileUrl;

    // 客戶上傳時的原始檔名，例如 "薪資證明.pdf"
    private String originalName;
}
