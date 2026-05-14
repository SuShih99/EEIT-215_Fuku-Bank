package com.javaeasybank.risk.dto.response;

import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskCheckResponse {
    private Disposition disposition;    // PASS / REJECT / MANUAL_REVIEW
    private RiskLevel riskLevel;
    private String reason;
    private Long logId;                 // 供外部模組存起來追蹤
    private Long reviewTaskId;          // 僅 MANUAL_REVIEW 時有值
    private String callBackUrl;
}

