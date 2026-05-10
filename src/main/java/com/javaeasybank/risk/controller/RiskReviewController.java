package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskReviewResponse;
import com.javaeasybank.risk.service.RiskReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        // dto 裡有 scene、businessId、customerId、callbackUrl、amount 等
        RiskReviewResponse result = riskReviewService.process(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
