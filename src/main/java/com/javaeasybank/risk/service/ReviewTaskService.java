package com.javaeasybank.risk.service;

import com.javaeasybank.risk.core.enums.BusinessScene;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.entity.ReviewTask;
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
    public void createTask(String businessId, BusinessScene scene, RiskLevel riskLevel) {
        ReviewTask task = new ReviewTask();
        task.setBusinessId(businessId);
        task.setScene(scene);
        task.setStatus("PENDING"); // 初始狀態為待處理

        Integer priority = switch (riskLevel) {
            case HIGH -> 1;
            case MEDIUM -> 5;
            case LOW -> 10;
            default -> 99;
        };
        task.setPriority(priority);

        rtRepos.save(task);
        log.info("[ReviewTask] 建立人工審核任務成功 | BusinessID: {} | Scene: {} | Priority: {}",
                businessId, scene, priority);
    }

}
