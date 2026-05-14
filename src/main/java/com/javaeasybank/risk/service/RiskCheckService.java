package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.request.RiskCheckRequest;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskCheckResponse;
import com.javaeasybank.risk.entity.ReviewTask;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.enums.BlacklistType;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import com.javaeasybank.risk.repository.RiskEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCheckService {


    private final BlackListService blackListService;
    private final RiskEventLogRepository relRepos;
    private final ReviewTaskService reviewTaskService;

    // 單筆閾值：5萬 → MANUAL_REVIEW
    private static final BigDecimal SINGLE_LIMIT = new BigDecimal("50000");

    @Transactional
    public RiskCheckResponse check(RiskCheckRequest request) {

        // 1. 黑名單比對（targetIdentifier 就是收款帳號）
        if (request.getTargetIdentifier() != null) {
            boolean hit = blackListService.isBlacklisted(
                    BlacklistType.ACCOUNT_NO,
                    request.getTargetIdentifier());

            if (hit) {
                log.warn("[RiskCheck] 命中黑名單 customerId={} target={}",
                        request.getCustomerId(), request.getTargetIdentifier());

                RiskEventLog eventLog = buildAndSaveLog(
                        request, RiskLevel.HIGH, Disposition.REJECT,
                        "收款帳號命中黑名單：" + request.getTargetIdentifier());

                return RiskCheckResponse.builder()
                        .disposition(Disposition.REJECT)
                        .riskLevel(RiskLevel.HIGH)
                        .reason("收款帳號已列入黑名單，交易終止")
                        .logId(eventLog.getLogId())
                        .build();
            }
        }

        // 2. 金額閾值判斷
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(SINGLE_LIMIT) >= 0) {
                log.info("[RiskCheck] 大額交易觸發人工審核 amount={}", request.getAmount());

                RiskEventLog eventLog = buildAndSaveLog(
                        request, RiskLevel.MEDIUM, Disposition.MANUAL_REVIEW,
                        "單筆金額 " + request.getAmount() + " 超過閾值 " + SINGLE_LIMIT);

                // 建立 ReviewTask
                Long reviewTaskId = null;
                if (request.getCallbackUrl() != null) {
                    ReviewTask task = reviewTaskService.createTask(eventLog, toReviewRequest(request));
                    reviewTaskId = task.getTaskId();
                }

                return RiskCheckResponse.builder()
                        .disposition(Disposition.MANUAL_REVIEW)
                        .riskLevel(RiskLevel.MEDIUM)
                        .reason("單筆金額超過 " + SINGLE_LIMIT + " 元，需人工審核")
                        .logId(eventLog.getLogId())
                        .reviewTaskId(reviewTaskId)
                        .build();
            }
        }

        // 3. 全部通過
        RiskEventLog eventLog = buildAndSaveLog(
                request, RiskLevel.LOW, Disposition.PASS, "通過系統自動規則檢查");

        return RiskCheckResponse.builder()
                .disposition(Disposition.PASS)
                .riskLevel(RiskLevel.LOW)
                .reason("通過系統自動規則檢查")
                .logId(eventLog.getLogId())
                .build();
    }

    // ── 私有方法 ──────────────────────────────────────

    private RiskEventLog buildAndSaveLog(
            RiskCheckRequest request,
            RiskLevel riskLevel,
            Disposition disposition,
            String reason) {

        RiskEventLog log = new RiskEventLog();
        log.setEventType(request.getScene().name());
        log.setBusinessId(request.getBusinessId() != null
                ? request.getBusinessId() : "UNKNOWN");
        log.setTargetIdentifier(request.getTargetIdentifier());
        log.setRiskLevel(riskLevel);
        log.setDisposition(disposition);
        log.setTriggerReason(reason);
        log.setTransactionAmount(request.getAmount());
        log.setCallbackUrl(request.getCallbackUrl());
        return relRepos.save(log);
    }

    /**
     * RiskCheckRequest → RiskReviewRequest
     * 供 reviewTaskService.createTask() 使用
     */
    private RiskReviewRequest toReviewRequest(RiskCheckRequest request) {
        RiskReviewRequest r = new RiskReviewRequest();
        r.setScene(request.getScene());
        r.setBusinessId(request.getBusinessId());
        r.setCustomerId(request.getCustomerId());
        r.setCallbackUrl(request.getCallbackUrl());
        r.setAmount(request.getAmount());
        return r;
    }
}