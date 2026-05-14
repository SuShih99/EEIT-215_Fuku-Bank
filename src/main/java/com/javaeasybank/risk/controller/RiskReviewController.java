package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskReviewResponse;
import com.javaeasybank.risk.service.RiskReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/risk/reviews")
@RequiredArgsConstructor
public class RiskReviewController {

    private final RiskReviewService riskReviewService;

    /**
     * 統一送審入口
     * 貸款模組：scene=LOAN_APPLY,   businessId=applicationId
     * 帳戶模組：scene=ACCOUNT_OPEN, businessId=accountId
     * 信用卡：  scene=CARD_APPLY,   businessId=cardApplicationId
     * <p>
     * POST /api/risk/reviews
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RiskReviewResponse>> submitReview(
            @RequestBody @Valid RiskReviewRequest dto) {
        log.info("[RiskReview] ===== 收到送審請求 =====");
        log.info("[RiskReview] scene={}, businessId={}, customerId={}",
                dto.getScene(), dto.getBusinessId(), dto.getCustomerId());
        log.info("[RiskReview] amount={}, callbackUrl={}", dto.getAmount(), dto.getCallbackUrl());
        log.info("[RiskReview] 選填欄位 annualIncome={}, occupation={}, externalScore={}, otherBankDebt={}, hasRealEstate={}",
                dto.getAnnualIncome(), dto.getOccupation(), dto.getExternalScore(),
                dto.getOtherBankDebt(), dto.getHasRealEstate());
        log.info("[RiskReview] 黑名單欄位 idCard={}, email={}, phone={}",
                dto.getIdCard(), dto.getEmail(), dto.getPhone());

        RiskReviewResponse result = riskReviewService.process(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
