package com.javaeasybank.risk.dto;

import com.javaeasybank.risk.core.RiskTarget;
import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.core.enums.ReviewResult;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.core.enums.BusinessScene;

import java.time.LocalDateTime;

/**
 * 風控人工審核事件
 *
 * @param scene      觸發場景 (例如：LOAN, CREATE_CUSTOMER)
 * @param businessId 業務主鍵 (例如：貸款申請單號, 客戶 ID)
 * @param target     包含 Metadata 的標的物件
 * @param reason     觸發人工審核的原因 (例如：信用評分灰區)
 */
public record ManualReviewEvent(
        BusinessScene scene,
        String businessId,
        RiskTarget target,
        RiskLevel level,
        Disposition disposition,
        String reason
//        BusinessScene scene,
//        String businessId,
//        RiskTarget target,
//        RiskLevel level,
//        Disposition disposition,
//        String status,
//        String reason,
//        ReviewResult reviewResult,
//        String assignee,
//        String adminComment,
//        Integer priority,
//        LocalDateTime createAt,
//        LocalDateTime processedAt
) {
}
