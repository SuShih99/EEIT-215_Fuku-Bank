package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.loan.dto.requests.LoanContactLogRequestDTO;
import com.javaeasybank.loan.dto.requests.LoanReviewDetailRequestDTO;
import com.javaeasybank.loan.dto.response.LoanApplicationResponseDTO;
import com.javaeasybank.loan.dto.response.LoanContactLogResponseDTO;
import com.javaeasybank.loan.dto.response.LoanReviewDetailResponseDTO;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行員端貸款案件管理 API。
 * 串接案件查詢、聯繫紀錄、二次填單、送審、核准、退件與補件等後台流程。
 */
@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class LoanAdminController {

    private final LoanApplicationService loanApplicationService;

    // 依申請狀態查詢申請列表
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getByStatus(
            @RequestParam(defaultValue = "PENDING_CONTACT") LoanApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getByStatus(status)));
    }

    // 為指定申請新增一筆聯繫紀錄
    @PostMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<Void>> addContactLog(
            @PathVariable String id,
            @RequestBody LoanContactLogRequestDTO dto) {
        loanApplicationService.addContactLog(id, dto);
        return ResponseEntity.status(201).body(ApiResponse.success(null));
    }

    // 查詢指定申請的所有聯繫紀錄
    @GetMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<List<LoanContactLogResponseDTO>>> getContactLogs(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getContactLogs(id)));
    }

    // 儲存行員二次填單（草稿模式，可多次覆寫）
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Void>> saveReview(
            @PathVariable String id,
            @RequestBody LoanReviewDetailRequestDTO dto) {
        loanApplicationService.saveReviewDetail(id, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 正式送審，觸發風控審核流程
    @PatchMapping("/{id}/review/submit")
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @PathVariable String id) {
        loanApplicationService.submitReview(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 查詢指定申請的二次填單詳情
    @GetMapping("/{id}/review")
    public ResponseEntity<ApiResponse<LoanReviewDetailResponseDTO>> getReview(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getReviewDetail(id)));
    }

    // 查詢最近被外部模組（風控 / 帳戶）異動過的申請
    @GetMapping("/recent-updates")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getRecentlyUpdated() {
        return ResponseEntity.ok(ApiResponse.success(
                loanApplicationService.getRecentlyUpdated()));
    }

    // 風控送審補償：手動重新送審至風控系統
    @PatchMapping("/{id}/risk/retry")
    public ResponseEntity<ApiResponse<Void>> retryRiskSubmit(@PathVariable String id) {
        loanApplicationService.retryRiskSubmit(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 撥款補償：手動重新觸發撥款流程
    @PatchMapping("/{id}/disburse/retry")
    public ResponseEntity<ApiResponse<Void>> retryDisburse(@PathVariable String id) {
        loanApplicationService.retryDisburse(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
