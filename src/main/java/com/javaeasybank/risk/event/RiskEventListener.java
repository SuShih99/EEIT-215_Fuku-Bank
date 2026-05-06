package com.javaeasybank.risk.event;

import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.dto.ManualReviewEvent;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.service.ReviewTaskService;
import com.javaeasybank.risk.service.RiskEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class RiskEventListener {

    private final RiskEventService reService;
    private final ReviewTaskService rtService;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleRiskManualReview(ManualReviewEvent event) {
        log.info("接收到審核事件，準備建立任務：{}", event.scene());

        RiskEventLog savedLog = reService.recordEvent(event);

        //只有 人工審核 才建立任務
        if (event.disposition() == Disposition.MANUAL_REVIEW) {
            rtService.createTask(savedLog, event);
        }
    }
}
