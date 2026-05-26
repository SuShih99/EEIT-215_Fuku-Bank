package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.RiskAttachmentRequest;
import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.dto.response.RiskReviewResponse;
import com.javaeasybank.risk.service.ReviewTaskService;
import com.javaeasybank.risk.service.RiskReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/risk/reviews")
@RequiredArgsConstructor
public class RiskReviewController {

    private final RiskReviewService riskReviewService;
    private final ReviewTaskService reviewTaskService;


    /**
     * 補件附件接收
     * 貸款模組在客戶送出補件後呼叫，將文件清單附加到對應的 ReviewTask
     * PATCH /api/risk/reviews/{businessId}/attachments
     */
    @PatchMapping("/{businessId}/attachments")
    public ResponseEntity<ApiResponse<Void>> attachDocuments(
            @PathVariable String businessId,
            @RequestBody RiskAttachmentRequest request) {

        log.info("[RiskAPI] 收到補件通知 businessId={}, 檔案數量={}",
                businessId, request.getDocuments() != null ? request.getDocuments().size() : 0);

        if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("補件清單不可為空"));
        }

        reviewTaskService.attachDocuments(businessId, request.getDocuments());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiskReviewResponse>> submitReview(
            @RequestBody @Valid RiskReviewRequest dto) {
        RiskReviewResponse result = riskReviewService.process(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
