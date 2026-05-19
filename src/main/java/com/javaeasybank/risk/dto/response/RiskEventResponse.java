package com.javaeasybank.risk.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RiskEventResponse {

    private Long logId;

    private String eventType;

    private String targetIdentifier;

    private RiskLevel riskLevel;

    private Disposition actionTaken;

    private String triggerReason;

    private String metaData;

    private BigDecimal transactionAmount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

}
