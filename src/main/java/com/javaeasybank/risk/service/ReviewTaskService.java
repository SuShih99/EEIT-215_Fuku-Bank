package com.javaeasybank.risk.service;

import com.javaeasybank.risk.core.enums.BusinessScene;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.entity.ReviewTask;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.repository.ReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class ReviewTaskService {

    private final ReviewTaskRepository rtRepos;

    public Page<ReviewTask> findAll(Pageable pageable) {
        return rtRepos.findAll(pageable);
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
}
