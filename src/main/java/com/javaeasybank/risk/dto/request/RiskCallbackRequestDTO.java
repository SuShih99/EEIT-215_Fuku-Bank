package com.javaeasybank.risk.dto.request;

import lombok.Data;

@Data
public class RiskCallbackRequestDTO {
    private String callerModule;  // "LOAN" | "CUSTOMER" | "ACCOUNT"
    private Long   targetId;      // 業務 ID
    private String newStatus;     // "APPROVED" | "REJECTED" | "FLAGGED"
    private String reason;        // 選填，給對方顯示用（例如：命中黑名單）
}
