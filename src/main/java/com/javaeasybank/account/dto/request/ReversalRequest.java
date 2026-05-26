package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 沖正請求 DTO，用於接收要沖正的原始交易編號。
 */
@Data
public class ReversalRequest {

    // 要沖正的原始交易編號
    @NotBlank
    private String originalReferenceId;

    // 沖正原因（選填）
    private String reason;
}
