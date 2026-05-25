package com.javaeasybank.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper; // 確保 ObjectMapper 已注入
    private final CreditScoreService creditScoreService;

    private static final BigDecimal SINGLE_LIMIT = new BigDecimal("50000"); // 單筆大額
    private static final BigDecimal STRUCTURING_THRESHOLD = new BigDecimal("45000"); // 拆單偵測 (90%)

    @Transactional
    public RiskCheckResponse check(RiskCheckRequest request) {

        // 1. 黑名單檢查
        RiskCheckResponse blacklistResult = checkBlacklist(request);
        if (blacklistResult != null)
            return blacklistResult; // 命中就提早結束
        // 【新增】PEP 政治敏感人士判斷
        // 說明：高優順序，一律轉人工審核（Disposition.MANUAL_REVIEW）
        Boolean pep = creditScoreService.getCreditInfoByCustomerId(request.getCustomerId()).getIsPep();
        if (Boolean.TRUE.equals(pep)) {
            log.info("[RiskCheck] 觸發 PEP 政治敏感人士法規合規檢查 customerId={}", request.getCustomerId());

            // 內部詳細原因 (存入資料庫)
            String internalDetail = "客戶具備 PEP (政治敏感人士) 身分，依據防制洗錢法規一律轉人工審核。";
            // 外部前端展示原因 (兼顧客戶體驗與資訊安全)
            String displayReason = "基於合規與交易安全考量，此筆交易已轉由人工審核中，請耐心等候。";

            RiskEventLog eventLog = buildAndSaveLog(
                    request, RiskLevel.MEDIUM, Disposition.MANUAL_REVIEW, internalDetail);

            // 自動觸發或建立人工審核工單 (可視現有架構調用 reviewTaskService)
            // reviewTaskService.createTask(eventLog);

            return buildRiskCheckResponse(eventLog, request, displayReason, Disposition.MANUAL_REVIEW, RiskLevel.MEDIUM);
        }

        // 處理來自 Account 模組的預判警告 (如：頻率過高、日限額接近)
        if (request.getContext() != null && request.getContext().containsKey("internalWarning")) {
            String warning = String.valueOf(request.getContext().get("internalWarning"));
            Disposition disposition;
            RiskLevel level;
            String displayReason;

            if (warning.contains("自動終止轉帳")) { // 高頻轉帳，直接拒絕
                disposition = Disposition.REJECT;
                level = RiskLevel.HIGH;
                displayReason = "系統偵測到異常交易行為，已自動終止此筆交易。";
            } else if (warning.contains("拆單") || warning.contains("頻繁")) { // 疑似拆單或頻繁小額，人工審核
                disposition = Disposition.MANUAL_REVIEW;
                level = RiskLevel.HIGH;
                displayReason = "系統偵測到您的帳戶有異常活動，為保障資金安全，此筆交易需轉由人工覆核。";
            } else { // 其他內部警告，例如單日累計超限
                disposition = Disposition.MANUAL_REVIEW;
                level = RiskLevel.MEDIUM;
                displayReason = "系統偵測到您的帳戶有異常活動，為保障資金安全，此筆交易需轉由人工覆核。";
            }

            RiskEventLog eventLog = buildAndSaveLog(
                    request, level, disposition, warning);
            return buildRiskCheckResponse(eventLog, request, displayReason, disposition, level);
        }

        // 處理登入檢查：針對帳號鎖定或認證失敗的嘗試，提升風險等級
        if (request.getContext() != null) {
            // 兼容多種可能的 Context Key (authStatus, status)
            Object statusObj = request.getContext().getOrDefault("authStatus",
                    request.getContext().getOrDefault("status",
                            request.getContext().get("eventType")));
            String status = statusObj != null ? String.valueOf(statusObj) : "";

            if ("FAILURE".equalsIgnoreCase(status) || "LOCK".equalsIgnoreCase(status)
                    || "LOCKED".equalsIgnoreCase(status)) {
                String reason = String.valueOf(request.getContext().getOrDefault("failReason",
                        request.getContext().getOrDefault("reason", "安全性異常登入嘗試 (帳號可能已鎖定)")));

                RiskEventLog eventLog = buildAndSaveLog(
                        request, RiskLevel.HIGH, Disposition.REJECT, reason);
                return RiskCheckResponse.builder()
                        .disposition(Disposition.REJECT)
                        .riskLevel(RiskLevel.HIGH)
                        .reason(eventLog.getTriggerReason())
                        .logId(eventLog.getLogId())
                        .build();
            } else if ("WARNING".equalsIgnoreCase(status)) {
                String reason = String.valueOf(request.getContext().getOrDefault("reason", "連續登入失敗警告"));
                RiskEventLog eventLog = buildAndSaveLog(request, RiskLevel.MEDIUM, Disposition.WARNING, reason);
                return RiskCheckResponse.builder()
                        .disposition(Disposition.WARNING)
                        .riskLevel(RiskLevel.MEDIUM)
                        .reason(reason)
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

                return buildRiskCheckResponse(eventLog, request, "此筆大額交易需轉由人工覆核，以確保您的帳戶資金安全，請耐心等候。",
                        Disposition.MANUAL_REVIEW,
                        RiskLevel.MEDIUM);
            }
        }

        // 3. 金額接近限額（Structuring）
        if (request.getAmount() != null
                && request.getAmount().compareTo(STRUCTURING_THRESHOLD) >= 0
                && request.getAmount().compareTo(SINGLE_LIMIT) < 0) {

            log.warn("[RiskCheck] 疑似拆單 customerId={} amount={}",
                    request.getCustomerId(), request.getAmount());

            RiskEventLog eventLog = buildAndSaveLog(
                    request, RiskLevel.HIGH, Disposition.MANUAL_REVIEW,
                    "交易金額接近限額（" + request.getAmount() + "），疑似拆單行為");

            return buildRiskCheckResponse(eventLog, request,
                    "基於合規與交易安全考量，此筆交易正由系統進行安全覆核中，我們將盡速為您處理。", Disposition.MANUAL_REVIEW,
                    RiskLevel.HIGH);
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
        if (request.getTargetIdentifier() == null) return null;

        String rawIdentifier = request.getTargetIdentifier().replace("轉入帳戶: ", "");

        boolean hit = blackListService.isBlacklisted(
                BlacklistType.ACCOUNT_NO,
                rawIdentifier);
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

        // 將 Request 中的 Context 完整記錄到 metaData 欄位
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                eventlog.setMetaData(objectMapper.writeValueAsString(request.getContext()));
            } catch (Exception e) {
                log.warn("[RiskCheck] metaData 序列化失敗 businessId={}", eventlog.getBusinessId(), e);
            }
        }

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

    private RiskCheckResponse buildRiskCheckResponse(
            RiskEventLog eventLog, RiskCheckRequest request, String displayReason, Disposition disposition,
            RiskLevel riskLevel) {

        Long reviewTaskId = null;
        // 只有在處置結果為「人工審核」時，才需要建立審核任務
        if (disposition == Disposition.MANUAL_REVIEW && request.getCallbackUrl() != null) {
            ReviewTask task = reviewTaskService.createTask(
                    eventLog, toReviewRequest(request));
            reviewTaskId = task.getTaskId();
        }

        return RiskCheckResponse.builder()
                .disposition(disposition)
                .riskLevel(riskLevel)
                .reason(displayReason != null ? displayReason : eventLog.getTriggerReason())
                .logId(eventLog.getLogId())
                .reviewTaskId(reviewTaskId)
                .build();
    }
}