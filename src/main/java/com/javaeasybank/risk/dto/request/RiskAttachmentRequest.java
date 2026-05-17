package com.javaeasybank.risk.dto.request;

import lombok.Data;

import java.util.List;
@Data
public class RiskAttachmentRequest {
    // 💡 明確宣告內部結構，對齊貸款端的 LoanDocumentInfoDTO 結構（或是用一個萬用的明細 DTO）
    private List<AttachmentDetail> documents;

    @Data
    public static class AttachmentDetail {
        private String documentId;
        private String documentUrl;
        private String documentType;
    }
}
