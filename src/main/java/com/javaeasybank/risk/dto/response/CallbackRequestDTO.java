package com.javaeasybank.risk.dto.response;

public class CallbackRequestDTO {
    private String newStatus;      // "APPROVED" or "REJECTED"
    private String callerModule;   // 固定填 "RISK"
    private String reason;         // 拒絕原因（選填）
}
