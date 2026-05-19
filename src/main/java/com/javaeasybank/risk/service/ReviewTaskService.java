package com.javaeasybank.risk.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.risk.dto.request.RiskAttachmentRequest;
import com.javaeasybank.risk.dto.response.ReviewTaskResponse;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.ReviewResult;
import com.javaeasybank.risk.dto.request.ReviewDecisionRequest;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.entity.ReviewTask;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.repository.ReviewTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class ReviewTaskService {

    private final ReviewTaskRepository rtRepos;
    private final CallbackService callbackService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ReviewTaskResponse> findAll(
            String status,
            BusinessScene scene,
            Integer priority,
            Pageable pageable) {

        return rtRepos.findByFilter(status, scene, priority, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ReviewTask createTask(RiskEventLog eventLog, RiskReviewRequest request) {

        ReviewTask task = new ReviewTask();
        task.setRiskEventLog(eventLog);                   // 補上關聯（entity 有這個欄位）
        task.setBusinessId(request.getBusinessId());
        task.setScene(request.getScene());
        task.setStatus("PENDING");
        task.setSubstatus("NEW");
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            task.setAttachments(objectMapper.writeValueAsString(request.getDocuments()));
        }

        Integer priority = switch (eventLog.getRiskLevel()) {
            case HIGH -> 1;
            case MEDIUM -> 5;
            case LOW -> 10;
            default -> 99;
        };
        task.setPriority(priority);

        ReviewTask saved = rtRepos.save(task);
        log.info("[ReviewTask] 建立成功 taskId={} businessId={} priority={}",
                saved.getTaskId(), saved.getBusinessId(), saved.getPriority());

        return saved;  // 回傳讓 handleDisposition 拿到 taskId
    }

    @Transactional
    public void makeDecision(Long taskId, ReviewDecisionRequest request) {

        ReviewTask task = rtRepos.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到審核任務：" + taskId));

        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("任務已結案，無法重複審核");
        }

        // 只有鎖定者本人才能送決策
        String auditor = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!auditor.equals(task.getAssignee())) {
            throw new BusinessException("此任務已被 " + task.getAssignee() + " 鎖定，您無法審核");
        }

        task.setReviewResult(request.getReviewResult());
        task.setAdminComment(request.getAdminComment());
        task.setAssignee(auditor);

        String newStatus = switch (request.getReviewResult()) {
            case APPROVED, REJECTED -> "COMPLETED";
            case RETURNED -> "PENDING";
        };
        task.setStatus(newStatus);

        if (request.getReviewResult() == ReviewResult.RETURNED) {
            // RETURNED 不設結案時間，保留補件要求供客戶端顯示。
            task.setSubstatus("WAITING_DOCUMENT");
            if (request.getRequiredDocuments() != null && !request.getRequiredDocuments().isEmpty()) {
                // 序列化存入，例如 ["INCOME_CERT","PROPERTY_CERT"]
                task.setRequiredDocumentsJson(objectMapper.writeValueAsString(request.getRequiredDocuments()));
            }
        } else {
            // APPROVED / REJECTED：結案，清除前一次退補件留下的狀態與文件要求。
            task.setSubstatus(null);
            task.setRequiredDocumentsJson(null);
            task.setProcessedAt(LocalDateTime.now());
        }

        rtRepos.save(task);

        log.info("[ReviewTask] 審核完成 taskId={} result={} assignee={}",
                taskId, request.getReviewResult(), auditor);

        triggerCallback(task);
    }

    private void triggerCallback(ReviewTask task) {

        RiskEventLog eventLog = task.getRiskEventLog();

        // ReviewResult → Disposition 轉換
        Disposition disposition = switch (task.getReviewResult()) {
            case APPROVED -> Disposition.PASS;
            case REJECTED -> Disposition.REJECT;
            case RETURNED -> Disposition.RETURN;
        };

        List<String> requiredDocs = null;
        if (task.getRequiredDocumentsJson() != null) {
            requiredDocs = objectMapper.readValue(
                    task.getRequiredDocumentsJson(),
                    new TypeReference<List<String>>() {}
            );
        }
        callbackService.notify(
                eventLog.getCallbackUrl(),
                disposition,
                eventLog, requiredDocs, task.getAdminComment());
    }

    @Transactional
    public void startProcessing(Long taskId, String currentAuditor) {
        ReviewTask task = rtRepos.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("找不到審核任務：" + taskId));

        // 1. 如果已經是 PROCESSING，且鎖定人不是自己，才阻擋
        if ("PROCESSING".equals(task.getStatus())) {
            if (task.getAssignee() != null && !task.getAssignee().equals(currentAuditor)) {
                throw new BusinessException("該案件已被其他審核人員處理中");
            }
            // 如果已經是 PROCESSING 且 assignee 就是自己，直接 return 即可，不需重複處理
            return;
        }

        // 2. 如果是已結案，絕對不允許再處理
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("該案件已結案，無法重新審核");
        }

        if ("PENDING".equals(task.getStatus()) && "WAITING_DOCUMENT".equals(task.getSubstatus())) {
            throw new BusinessException("該案件目前待客戶補件，補件送出前不可開始審核");
        }

        task.setStatus("PROCESSING");
        task.setSubstatus("IN_REVIEW");
        task.setAssignee(currentAuditor);

        rtRepos.save(task);

        log.info("[ReviewTask] 案件已被鎖定開始處理 taskId={} auditor={}", taskId, currentAuditor);
    }

    @Transactional
    public void attachDocuments(String businessId, List<RiskAttachmentRequest.AttachmentDetail> documents) {
        ReviewTask task = rtRepos
                .findFirstByBusinessIdAndStatusAndSubstatusOrderByCreateAtDesc(
                        businessId, "PENDING", "WAITING_DOCUMENT")
                .orElseThrow(() -> new BusinessException("找不到待補件審核任務：" + businessId));
        try {
            task.setAttachments(objectMapper.writeValueAsString(documents));

            task.setSubstatus("RESUBMITTED");

            // 如果原本是低優先級(>2)，動態提升任務優先級至 2；若原本就是高風險(1或2)則保持不變
            if (task.getPriority() != null && task.getPriority() > 2) {
                task.setPriority(2);
                log.info("[ReviewTask] 客戶已完成補件，動態提升任務優先級至 2 (High Priority)");
            }

            task.setAssignee(null);

            rtRepos.save(task);
            log.info("[ReviewTask] 補件附件已更新，案件已成功釋放。taskId={} businessId={}", task.getTaskId(), businessId);
        } catch (Exception e) {
            log.error("[ReviewTask] 補件附件序列化失敗 businessId={}", businessId, e);
        }
    }

    private ReviewTaskResponse toResponse(ReviewTask task) {
        RiskEventLog eventLog = task.getRiskEventLog();
        ReviewTaskResponse.ReviewTaskResponseBuilder builder = ReviewTaskResponse.builder()
                .taskId(task.getTaskId())
                .businessId(task.getBusinessId())
                .scene(task.getScene())
                .status(task.getStatus())
                .substatus(task.getSubstatus())
                .reviewResult(task.getReviewResult())
                .assignee(task.getAssignee())
                .adminComment(task.getAdminComment())
                .priority(task.getPriority())
                .createAt(task.getCreateAt())
                .processedAt(task.getProcessedAt())
                .attachments(task.getAttachments());
        if (eventLog != null) {
            builder.riskLevel(eventLog.getRiskLevel())           // ← 確認有這行
                    .triggerReason(eventLog.getTriggerReason())   // ← 確認有這行
                    .transactionAmount(eventLog.getTransactionAmount())
                    .metaData(eventLog.getMetaData())
                    .logCreatedAt(eventLog.getCreatedAt());
        }
        return builder.build();
    }
}
