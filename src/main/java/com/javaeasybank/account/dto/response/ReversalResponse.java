package com.javaeasybank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 沖正響應 DTO，回傳沖正後的結果資訊。
 */
@Data
public class ReversalResponse {

    // 新的沖正交易編號
    private String reversalReferenceId;

    // 被沖正的原始交易編號
    private String originalReferenceId;

    // 沖正明細（每個被沖正的帳戶一筆）
    private List<ReversalDetail> details;

    // 沖正時間
    private LocalDateTime reversedAt;

    @Data
    public static class ReversalDetail {
        private String accountNumber;
        private BigDecimal reversedAmount;
        private BigDecimal balanceAfter;
    }
}
