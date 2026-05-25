package com.javaeasybank.risk.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.ReviewResult;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Builder
public class ReviewTaskResponse {

    private Long taskId;
    private String businessId;
    private BusinessScene scene;
    private String status;
    private String substatus;
    private ReviewResult reviewResult;
    private String assignee;
    private String adminComment;
    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime processedAt;
    private Long logId;
    private RiskLevel riskLevel;
    private Disposition disposition;
    private String triggerReason;
    private String metaData;
    private BigDecimal transactionAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime logCreatedAt;

    /** 客戶送出的補件文件清單（JSON 陣列字串，null = 尚未收到補件） */
    private String attachments;
}
