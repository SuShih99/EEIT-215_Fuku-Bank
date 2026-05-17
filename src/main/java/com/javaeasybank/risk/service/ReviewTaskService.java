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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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

        String auditor = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        task.setReviewResult(request.getReviewResult());
        task.setAdminComment(request.getAdminComment());
        task.setAssignee(auditor);

        String newStatus = switch (request.getReviewResult()) {
            case APPROVED, REJECTED -> "COMPLETED";
            case RETURNED -> "PENDING";
        };
        task.setStatus(newStatus);

        // RETURNED 不設結案時間
        if (request.getReviewResult() != ReviewResult.RETURNED) {
            task.setProcessedAt(LocalDateTime.now());
        }

        rtRepos.save(task);

        log.info("[ReviewTask] 審核完成 taskId={} result={} assignee={}",
                taskId, request.getReviewResult(), auditor);

        triggerCallback(task);
    }

    private void triggerCallback(ReviewTask task) {

        // RETURNED 不 callback，等客戶補件後重新審核
        if (task.getReviewResult() == ReviewResult.RETURNED) {
            log.info("[ReviewTask] 退回補件 taskId={} 不觸發 callback",
                    task.getTaskId());
            return;
        }
        RiskEventLog eventLog = task.getRiskEventLog();

        // ReviewResult → Disposition 轉換
        Disposition disposition = switch (task.getReviewResult()) {
            case APPROVED -> Disposition.PASS;
            case REJECTED -> Disposition.REJECT;
            case RETURNED -> throw new IllegalStateException("不應該到這裡");
        };

        callbackService.notify(
                eventLog.getCallbackUrl(),   // ← 問題在這
                disposition,
                eventLog);
    }

    @Transactional
    public void attachDocuments(String businessId, List<RiskAttachmentRequest.AttachmentDetail> documents) {
        ReviewTask task = rtRepos.findFirstByBusinessIdAndStatusOrderByCreateAtDesc(businessId,"PENDING").orElse(null);
        if (task == null) {
            log.warn("[ReviewTask] 找不到對應任務，略過補件附件 businessId={}", businessId);
            return;
        }
        try {
            task.setAttachments(objectMapper.writeValueAsString(documents));
            // 動態提高該任務的優先級（Priority）
            //這樣這筆案子就會在前端列表彈到最上方，提醒審核人員：「客戶補件了，請優先覆審！」
            if (task.getPriority() != null && task.getPriority() > 2) {
                task.setPriority(2);
                log.info("[ReviewTask] 客戶已完成補件，動態提升任務優先級至 2 (High Priority)");
            }
            rtRepos.save(task);
            log.info("[ReviewTask] 補件附件已更新 taskId={} businessId={}", task.getTaskId(), businessId);
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