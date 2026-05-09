package com.javaeasybank.risk.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.risk.core.enums.BlacklistType;
import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.dto.request.RiskReviewRequestDTO;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import com.javaeasybank.risk.entity.RiskEventLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LoanReviewService {

    private final BlackListService blService;
    private final ReviewTaskService rtService;
    private final RiskEventService reService;
    private final CreditSCoreService csService;
    private final CallBackService cbService;

    @Transactional
    public void review(RiskReviewRequestDTO dto) {

        String businessId = String.valueOf(dto.getSourceId());
        RiskLevel riskLevel;
        Disposition disposition;
        String reason;

        // 1. 查黑名單（身分證號 / 手機 / email 都可以帶進來）
        boolean blocked = blService.isBlacklisted(
                BlacklistType.PHONE, dto.getApplicantPhone())
                || blService.isBlacklisted(
                BlacklistType.EMAIL, dto.getApplicantEmail());

        if (blocked) {
            riskLevel = RiskLevel.HIGH;
            disposition = Disposition.REJECT;
            reason = "申請人命中黑名單";

        } else {
            // 2. 取信用評分
            CustomerCreditInfo credit = csService
                    .getOrDefault(dto.getApplicantId());
            int score = credit.getFinalScore() != null ? credit.getFinalScore() : 0;

            // 3. 評分邏輯
            if (score >= 70) {
                riskLevel = RiskLevel.LOW;
                disposition = Disposition.PASS;
                reason = "信用評分達標";
            } else if (score >= 40) {
                riskLevel = RiskLevel.MEDIUM;
                disposition = Disposition.MANUAL_REVIEW;
                reason = "信用評分偏低，需人工複核";
            } else {
                riskLevel = RiskLevel.HIGH;
                disposition = Disposition.REJECT;
                reason = "信用評分不足";
            }
        }

        // 4. 寫 log
        RiskEventLog eventLog = reService.record(
                "LOAN_SUBMIT", businessId, dto.getApplicantId(),
                riskLevel, disposition, reason,
                buildMeta(dto), dto.getRequestedAmount());

        // 5. 後續處理
        if (disposition == Disposition.MANUAL_REVIEW) {
            reviewTaskService.createTask(
                    eventLog, businessId, BusinessScene.LOAN, 2);
            // 人工審核不打 callback，等行員 complete() 後再打
        } else {
            String newStatus = disposition == Disposition.PASS
                    ? "APPROVED" : "REJECTED";
            callbackService.sendCallback(
                    dto.getCallbackUrl(), "LOAN",
                    dto.getTargetId(), newStatus, reason);
        }
    }

    private String buildMeta(RiskReviewRequestDTO dto) {
        return String.format("{\"requestedAmount\":%s,\"loanTermMonths\":%d}",
                dto.getRequestedAmount(), dto.getLoanTermMonths());

    }
}