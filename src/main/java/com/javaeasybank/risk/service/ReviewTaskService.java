package com.javaeasybank.risk.service;

import com.javaeasybank.common.exception.BusinessException;
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

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class ReviewTaskService {

    private final ReviewTaskRepository rtRepos;
    private final CallbackService callbackService;

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
                .processedAt(task.getProcessedAt());
        return builder.build();
    }
}