package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.ManualReviewEvent;
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
    public void createTask(RiskEventLog savedLog, ManualReviewEvent event) {
        ReviewTask task = new ReviewTask();
        task.setRiskEventLog(savedLog);
        task.setBusinessId(event.businessId());
        task.setScene(event.scene());
        task.setStatus("PENDING");

        Integer priority = switch (event.level()) {
            case HIGH -> 1;
            case MEDIUM -> 5;
            case LOW -> 10;
            default -> 99;
        };
        task.setPriority(priority);

        rtRepos.save(task);
        log.info("已成功建立人工審核任務，BusinessID: {}", event.businessId());
    }

}
