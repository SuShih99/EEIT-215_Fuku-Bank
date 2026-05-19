package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskReviewResponse;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskReviewService {

    private final BlackListService blackListService;
    private final CreditScoreService creditScoreService;
    private final ReviewTaskService reviewTaskService;
    private final CallbackService callbackService;
    private final RiskEventLogRepository logRepository;

    @Transactional
    public RiskReviewResponse process(RiskReviewRequest request) {

        // 1. 黑名單批次檢查
        List<BlacklistType> hitTypes = blackListService.checkAll(buildBlacklistMap(request));
        if (!hitTypes.isEmpty()) {
            log.info("[RiskReview] 命中黑名單 businessId={} hitTypes={}",
                    request.getBusinessId(), hitTypes);
            RiskEventLog log = buildAndSaveLog(
                    request, null, 0,
                    Disposition.REJECT,
                    "命中黑名單：" + hitTypes);
            // 黑名單直接 callback，不需要人審
            callbackService.notify(request.getCallbackUrl(), Disposition.REJECT, log);
            return buildResponse(log, Disposition.REJECT, null, 0);
        }

        // 2. 同步信用資料並重新評分
        //    - 客戶不存在 → initializeCreditInfo 建立 mock 資料
        //    - 客戶已存在 → 覆蓋選填欄位後 rescore
        CustomerCreditInfo credit = syncCredit(request);

        // 3. 依 finalScore 決定 Disposition
        //    CreditSCoreService 的 resolveRiskLevel：
        //    ≥ 70 → LOW, 40~69 → MEDIUM, < 40 → HIGH
        //    對應送審閾值：LOW → PASS, MEDIUM → MANUAL_REVIEW, HIGH → REJECT
        Disposition disposition = resolveDisposition(credit);

        log.info("[RiskReview] businessId={} finalScore={} riskLevel={} disposition={}",
                request.getBusinessId(), credit.getFinalScore(),
                credit.getRiskLevel(), disposition);

        // 4. 存 RiskEventLog + 後續動作（同一個 @Transactional 保護）
        return handleDisposition(request, credit, disposition);
    }

    // ── 私有方法 ─────────────────────────────────────────────────────────

    /**
     * 從 dto 組黑名單比對 Map
     * null 的欄位不放入，避免 checkAll 誤判
     */
    private Map<BlacklistType, String> buildBlacklistMap(RiskReviewRequest dto) {
        Map<BlacklistType, String> map = new EnumMap<>(BlacklistType.class);
        if (dto.getIdCard() != null) map.put(BlacklistType.ID_CARD, dto.getIdCard());
        if (dto.getPhone() != null) map.put(BlacklistType.PHONE, dto.getPhone());
        if (dto.getEmail() != null) map.put(BlacklistType.EMAIL, dto.getEmail());
        return map;
    }

    private CustomerCreditInfo syncCredit(RiskReviewRequest dto) {

        // 不再處理新客戶建立，找不到直接拋例外
        // 代表 Customer 模組尚未完成初始化，屬於異常情況
        //creditScoreService.updateIfPresent(dto);
        return creditScoreService.rescore(dto.getCustomerId());
    }

    /**
     * RiskLevel → Disposition 對應規則
     * LOW    → PASS          自動通過
     * MEDIUM → MANUAL_REVIEW 轉人工審核
     * HIGH   → REJECT        自動拒絕
     */
    private Disposition resolveDisposition(CustomerCreditInfo credit) {
        return switch (credit.getRiskLevel()) {
            case LOW -> Disposition.PASS;
            case MEDIUM -> Disposition.MANUAL_REVIEW;
            case HIGH -> Disposition.REJECT;
            default -> throw new IllegalStateException("未知的風險等級: " + credit.getRiskLevel());

        };
    }

    /**
     * 存 Log 後依 Disposition 分流
     * - PASS / REJECT    → 立刻 callback
     * - MANUAL_REVIEW    → 建 ReviewTask，等人工決策後再 callback
     */
    private RiskReviewResponse handleDisposition(
            RiskReviewRequest dto,
            CustomerCreditInfo credit,
            Disposition disposition) {

        RiskEventLog eventLog = buildAndSaveLog(
                dto, credit,
                credit.getFinalScore(),
                disposition, null);

        return switch (disposition) {
            case PASS, REJECT -> {
                callbackService.notify(dto.getCallbackUrl(), disposition, eventLog);
                yield buildResponse(eventLog, disposition, null, credit.getFinalScore());
            }
            case MANUAL_REVIEW -> {
                ReviewTask task = reviewTaskService.createTask(eventLog, dto);
                yield buildResponse(eventLog, disposition, task.getTaskId(), credit.getFinalScore());
            }
            case RETURN -> {
                log.error("[RiskEngine] 自動風控引擎不應直接計算出退回補件狀態 applicationId={}", dto.getBusinessId());
                throw new IllegalStateException("自動規則審查不支援直接導向退回補件流程");
            }
            case WARNING -> {
                // 警告不阻斷流程，僅記錄 log，不 callback 也不建 ReviewTask
                log.warn("[RiskEngine] 非預期的 WARNING disposition 出現在信用審核流程 businessId={}", dto.getBusinessId());
                throw new IllegalStateException("信用審核流程不支援 WARNING disposition");
            }
        };
    }

    private RiskEventLog buildAndSaveLog(
            RiskReviewRequest dto,
            CustomerCreditInfo credit,
            int finalScore,
            Disposition disposition,
            String overrideReason) {

        RiskEventLog log = new RiskEventLog();
        log.setEventType(dto.getScene().name());
        log.setBusinessId(dto.getBusinessId());
        log.setTargetIdentifier(dto.getCustomerId());
        log.setRiskLevel(credit != null ? credit.getRiskLevel() : RiskLevel.HIGH);
        log.setDisposition(disposition);
        log.setTransactionAmount(dto.getAmount());
        log.setTriggerReason(overrideReason != null
                ? overrideReason
                : buildReason(credit, finalScore));
        log.setMetaData(credit != null ? buildMetaData(credit) : null);
        log.setCallbackUrl(dto.getCallbackUrl());
        return logRepository.save(log);
    }

    private String buildReason(CustomerCreditInfo credit, int finalScore) {
        if (credit == null) return null;
        return String.format(
                "{\"finalScore\":%d,\"riskLevel\":\"%s\",\"occupation\":\"%s\"," +
                        "\"annualIncome\":%s,\"externalScore\":%d," +
                        "\"otherBankDebt\":%s,\"hasRealEstate\":%b,\"isPep\":%b}",
                finalScore,
                credit.getRiskLevel(),
                credit.getOccupation(),
                credit.getAnnualIncome(),
                credit.getExternalScore(),
                credit.getOtherBankDebt(),
                credit.getHasRealEstate(),
                credit.getIsPep());
    }

    /**
     * 把關鍵評分依據序列化進 metaData（方便事後查核）
     * 對應 RiskEventLog.metaData 欄位
     */
    private String buildMetaData(CustomerCreditInfo credit) {
        return String.format(
                "{\"finalScore\":%d,\"externalScore\":%d,\"annualIncome\":%s," +
                        "\"otherBankDebt\":%s,\"hasRealEstate\":%b,\"occupation\":\"%s\"}",
                credit.getFinalScore(),
                credit.getExternalScore(),
                credit.getAnnualIncome(),
                credit.getOtherBankDebt(),
                credit.getHasRealEstate(),
                credit.getOccupation());
    }

    private RiskReviewResponse buildResponse(
            RiskEventLog log,
            Disposition disposition,
            Long reviewTaskId, int finalScore) {

        return RiskReviewResponse.builder()
                .logId(log.getLogId())
                .businessId(log.getBusinessId())
                .disposition(disposition)
                .riskLevel(log.getRiskLevel())
                .finalScore(finalScore)
                .reason(log.getTriggerReason())
                .reviewTaskId(reviewTaskId)
                .build();
    }

}
