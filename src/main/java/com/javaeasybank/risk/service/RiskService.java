package com.javaeasybank.risk.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.risk.dto.request.RiskReviewRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final LoanReviewService     lrService;
    //private final CustomerReviewService customerReviewService;
    //private final AccountReviewService  accountReviewService;

    public void processReview(RiskReviewRequestDTO dto) {
        switch (dto.getReviewType()) {
            case "LOAN_APPLICATION" -> lrService.review(dto);
            //case "KYC"              -> customerReviewService.review(dto);
            //case "ABNORMAL_TX"      -> accountReviewService.review(dto);
            default -> throw new BusinessException(
                    "未知審核類型: " + dto.getReviewType());
        }
    }
}