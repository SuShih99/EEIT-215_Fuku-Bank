package com.javaeasybank.risk.handler;

import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.risk.core.RiskHandler;
import com.javaeasybank.risk.core.RiskTarget;
import com.javaeasybank.risk.core.enums.BusinessScene;
import com.javaeasybank.risk.service.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoanApplyHandler implements RiskHandler {

    // 建議新增一個專門處理信用評分的 Service
    //private final CreditScoringService scoringService;

    private final LoanApplicationRepository laRepo;
    private final BlackListService blService;

    public LoanApplyHandler(ApplicationEventPublisher eventPublisher,
                            LoanApplicationRepository laRepo,
                            BlackListService blService) {
        super();
        this.laRepo = laRepo;
        this.blService = blService;
    }

    @Override
    public BusinessScene getScene() {
        return BusinessScene.LOAN_APPLY;
    }

    @Override
    public void handle(RiskTarget target) {

    }

    @Override
    public RiskTarget resolve(Object[] args) {

        if (args == null || args.length == 0) return null;

        // 如果第一個參數是 String (即 applicationId)
        if (args[0] instanceof
                String appId) {
            // 從資料庫查出最新的貸款申請資料
//            return laRepo.findById(appId)
//                    .map(this::toRiskTarget)
//                    .orElseGet(() -> {
//                        log.error("風控攔截失敗：找不到申請編號 {}", appId);
//                        return null;
//                    });
        }
        return null;
    }
}
