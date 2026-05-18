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

    private static final BigDecimal SINGLE_LIMIT = new BigDecimal("50000"); // 單筆大額
    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("45000"); // 拆單偵測 (90%)

    @Transactional
    public RiskCheckResponse check(RiskCheckRequest request) {

        // 1. 黑名單檢查
        RiskCheckResponse blacklistResult = checkBlacklist(request);
        if (blacklistResult != null)
            return blacklistResult; // 命中就提早結束

        // 處理來自 Account 模組的預判警告 (如：頻率過高、日限額接近)
        if (request.getContext() != null && request.getContext().containsKey("internalWarning")) {
            String warning = String.valueOf(request.getContext().get("internalWarning"));
            // 拆單是高風險
            RiskLevel level = warning.contains("拆單") || warning.contains("頻繁")
                    ? RiskLevel.HIGH
                    : RiskLevel.MEDIUM;

            RiskEventLog eventLog = buildAndSaveLog(
                    request, level, Disposition.MANUAL_REVIEW, warning);
            return buildManualReviewResponse(eventLog, request);
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
                    ReviewTask task = reviewTaskService.createTask(eventLog,
                            toReviewRequest(request));
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

        // 3. 金額接近限額（Structuring）
        if (request.getAmount() != null
                && request.getAmount().compareTo(STRUCTURING_THRESHOLD) >= 0
                && request.getAmount().compareTo(SINGLE_LIMIT) < 0) {

            log.warn("[RiskCheck] 疑似拆單 customerId={} amount={}",
                    request.getCustomerId(), request.getAmount());

            RiskEventLog eventLog = buildAndSaveLog(
                    request, RiskLevel.MEDIUM, Disposition.MANUAL_REVIEW,
                    "交易金額接近限額（" + request.getAmount() + "），疑似拆單行為");

            return buildManualReviewResponse(eventLog, request);
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

    private RiskCheckResponse checkBlacklist(RiskCheckRequest request) {
        if (request.getTargetIdentifier() == null) {
            return null;
        }
        boolean hit = blackListService.isBlacklisted(
                BlacklistType.ACCOUNT_NO,
                request.getTargetIdentifier());
        if (hit) {
            // 1. 準備內部理由（存資料庫用）
            String internalDetail = "收款帳號命中黑名單：" + request.getTargetIdentifier();

            // 2. 準備外部理由（回傳給帳戶模組用）
            String displayReason = "此轉入帳號已被通報為異常帳戶或列入警示名單";

            log.warn("[RiskCheck] 命中黑名單 customerId={} target={} reason={}",
                    request.getCustomerId(), request.getTargetIdentifier(), internalDetail);

            // 存入 RiskEventLog (存 internalDetail)
            RiskEventLog eventLog = buildAndSaveLog(
                    request, RiskLevel.HIGH, Disposition.REJECT, internalDetail);

            // 回傳 Response (回傳 displayReason)
            return RiskCheckResponse.builder()
                    .disposition(Disposition.REJECT)
                    .riskLevel(RiskLevel.HIGH)
                    .reason(displayReason) // 不要直接把黑名單編號噴給用戶
                    .logId(eventLog.getLogId())
                    .build();

        }
        return null;
    }


    private RiskEventLog buildAndSaveLog(
            RiskCheckRequest request,
            RiskLevel riskLevel,
            Disposition disposition,
            String reason) {

        RiskEventLog eventlog = new RiskEventLog();
        eventlog.setEventType(request.getScene().name());
        eventlog.setBusinessId(request.getBusinessId() != null
                ? request.getBusinessId()
                : "UNKNOWN");
        eventlog.setTargetIdentifier(request.getTargetIdentifier());
        eventlog.setRiskLevel(riskLevel);
        eventlog.setDisposition(disposition);
        eventlog.setTriggerReason(reason);
        eventlog.setTransactionAmount(request.getAmount());
        eventlog.setCallbackUrl(request.getCallbackUrl());
        return relRepos.save(eventlog);
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

    private RiskCheckResponse buildManualReviewResponse(
            RiskEventLog eventLog, RiskCheckRequest request) {

        Long reviewTaskId = null;
        if (request.getCallbackUrl() != null) {
            ReviewTask task = reviewTaskService.createTask(
                    eventLog, toReviewRequest(request));
            reviewTaskId = task.getTaskId();
        }

        return RiskCheckResponse.builder()
                .disposition(Disposition.MANUAL_REVIEW)
                .riskLevel(eventLog.getRiskLevel())
                .reason(eventLog.getTriggerReason())
                .logId(eventLog.getLogId())
                .reviewTaskId(reviewTaskId)
                .build();
    }
}