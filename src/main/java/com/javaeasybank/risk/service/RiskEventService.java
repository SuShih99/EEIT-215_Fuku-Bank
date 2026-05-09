package com.javaeasybank.risk.service;

import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskEventResponse;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.repository.RiskEventLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RiskEventService {

    private final RiskEventLogRepository relRepos;
    private final ObjectMapper objectMapper; // 用於將 Map 轉為 JSON 字串

    public Page<RiskEventResponse> findAll(Pageable pageable) {
        return relRepos.findAll(pageable)
                .map(this::toDto);
    }

    public Page<RiskEventResponse> search(String eventType, String actionTaken, String riskLevel, Pageable pageable) {
        return relRepos.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(eventType)) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (StringUtils.hasText(actionTaken)) {
                predicates.add(cb.equal(root.get("actionTaken"), actionTaken));
            }
            if (StringUtils.hasText(riskLevel)) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(this::toDto);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Async
    public void recordEvent(RiskReviewRequest request) {
        log.info("[RiskEvent] 異步開始記錄風險事件: businessId={}, type={}",
                request.getBusinessId(), request.getBusinessType());

        try {
            RiskEventLog logEntry = new RiskEventLog();

            // 1. 映射基礎欄位
            logEntry.setBusinessId(request.getBusinessId());
            logEntry.setTargetIdentifier(request.getCustomerId());
            logEntry.setEventType(request.getBusinessType() + "_SUBMIT");

            // 2. 設定初始風控狀態
            logEntry.setRiskLevel(RiskLevel.LOW); // 預設等級
            logEntry.setDisposition(Disposition.MANUAL_REVIEW); // 預設進入人工審核
            logEntry.setTriggerReason("系統接收 " + request.getBusinessType() + " 送審請求");

            // 3. 處理 Map 資料存入 metaData (JSON)
            if (request.getBusinessDetails() != null) {
                // 將 businessDetails Map 序列化為 JSON 字串
                String jsonDetails = objectMapper.writeValueAsString(request.getBusinessDetails());
                logEntry.setMetaData(jsonDetails);

                // 嘗試抓取金額 (自動從 details 裡找 common keys)
                Object amountObj = request.getBusinessDetails().getOrDefault("confirmedAmount",
                        request.getBusinessDetails().get("amount"));
                if (amountObj != null) {
                    logEntry.setTransactionAmount(new java.math.BigDecimal(amountObj.toString()));
                }
            }

            // 存檔
            relRepos.save(logEntry);
            log.info("[RiskEvent] 日誌記錄成功: logId={}", logEntry.getLogId());

        } catch (Exception e) {
            log.error("[RiskEvent] 記錄失敗，原因: {}", e.getMessage(), e);
            // 異步方法內的異常不會拋回給呼叫方，所以這裡要記錄清楚
        }
    }

    private RiskEventResponse toDto(RiskEventLog rel) {
        RiskEventResponse response = new RiskEventResponse();
        response.setLogId(rel.getLogId());
        response.setEventType(rel.getEventType());
        response.setRiskLevel(rel.getRiskLevel());
        response.setTargetIdentifier(rel.getTargetIdentifier());
        response.setActionTaken(rel.getDisposition());
        response.setTriggerReason(rel.getTriggerReason());
        response.setTransactionAmount(rel.getTransactionAmount());
        response.setCreatedAt(rel.getCreatedAt());

        return response;
    }
}
