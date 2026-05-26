package com.javaeasybank.risk.dto.request;

import lombok.Data;

import java.util.List;
@Data
public class RiskAttachmentRequest {
    private List<AttachmentDetail> documents;

    @Data
    public static class AttachmentDetail {
        private String documentId;
        private String fileUrl;
        private String documentType;
        private String originalName;
    }
}
