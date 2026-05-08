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

// 行員端：管理入口
@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class LoanAdminController {

    private final LoanApplicationService loanApplicationService;

    // 依狀態查詢申請列表（預設 PENDING_CONTACT）
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getByStatus(
            @RequestParam(defaultValue = "PENDING_CONTACT") LoanApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getByStatus(status)));
    }

    // 新增聯繫紀錄
    @PostMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<Void>> addContactLog(
            @PathVariable String id,
            @RequestBody LoanContactLogRequestDTO dto) {
        loanApplicationService.addContactLog(id, dto);
        return ResponseEntity.status(201).body(ApiResponse.success(null));
    }

    // 查詢聯繫紀錄
    @GetMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<List<LoanContactLogResponseDTO>>> getContactLogs(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getContactLogs(id)));
    }

    // 儲存二次填單草稿
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Void>> saveReview(
            @PathVariable String id,
            @RequestBody LoanReviewDetailRequestDTO dto) {
        loanApplicationService.saveReviewDetail(id, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 送審
    @PatchMapping("/{id}/review/submit")
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @PathVariable String id) {
        loanApplicationService.submitReview(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 查詢填單內容
    @GetMapping("/{id}/review")
    public ResponseEntity<ApiResponse<LoanReviewDetailResponseDTO>> getReview(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getReviewDetail(id)));
    }

    // 置頂：最近被風控或帳戶模組異動的申請
    @GetMapping("/recent-updates")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getRecentlyUpdated() {
        return ResponseEntity.ok(ApiResponse.success(
                loanApplicationService.getRecentlyUpdated()));
    }
}
