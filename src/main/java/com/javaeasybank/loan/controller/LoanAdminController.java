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
 * 行員端貸款申請管理 Controller。
 *
 * <p>提供行員操作貸款申請的完整 REST API，包含申請查詢、聯繫紀錄管理、
 * 二次填單、送審及補償機制。</p>
 *
 * <p>Base URL：{@code /api/admin/loan-applications}</p>
 * <ul>
 *   <li>{@code GET    /}                    — 依狀態查詢申請列表</li>
 *   <li>{@code GET    /recent-updates}       — 查詢最近被外部模組異動的申請（置頂）</li>
 *   <li>{@code POST   /{id}/contact-logs}    — 新增聯繫紀錄</li>
 *   <li>{@code GET    /{id}/contact-logs}    — 查詢聯繫紀錄</li>
 *   <li>{@code POST   /{id}/review}          — 儲存二次填單草稿</li>
 *   <li>{@code PATCH  /{id}/review/submit}   — 正式送審（觸發風控流程）</li>
 *   <li>{@code GET    /{id}/review}          — 查詢填單內容</li>
 *   <li>{@code PATCH  /{id}/risk/retry}      — 風控送審補償（手動重送）</li>
 *   <li>{@code PATCH  /{id}/disburse/retry}  — 撥款補償（手動重送）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class LoanAdminController {

    private final LoanApplicationService loanApplicationService;

    /**
     * 依申請狀態查詢申請列表。
     *
     * <p>不帶 {@code status} 參數時預設顯示 {@code PENDING_CONTACT}（待聯絡）的申請。</p>
     *
     * @param status 申請狀態（預設 {@code PENDING_CONTACT}）
     * @return 符合狀態的申請清單，依建立時間降序排列
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getByStatus(
            @RequestParam(defaultValue = "PENDING_CONTACT") LoanApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getByStatus(status)));
    }

    /**
     * 為指定申請新增一筆聯繫紀錄。
     *
     * <p>新增後同步更新 {@code LoanApplication.latestContactStatus} 與
     * {@code latestContactTime} 快照欄位。</p>
     *
     * @param id  申請識別碼
     * @param dto 聯繫紀錄資料（行員工號、聯繫結果、管道、備註）
     * @return HTTP 201 Created
     */
    @PostMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<Void>> addContactLog(
            @PathVariable String id,
            @RequestBody LoanContactLogRequestDTO dto) {
        loanApplicationService.addContactLog(id, dto);
        return ResponseEntity.status(201).body(ApiResponse.success(null));
    }

    /**
     * 查詢指定申請的所有聯繫紀錄。
     *
     * @param id 申請識別碼
     * @return 聯繫紀錄清單，依聯繫時間降序排列（最新的在前）
     */
    @GetMapping("/{id}/contact-logs")
    public ResponseEntity<ApiResponse<List<LoanContactLogResponseDTO>>> getContactLogs(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getContactLogs(id)));
    }

    /**
     * 儲存行員二次填單（草稿模式，可多次覆寫）。
     *
     * <p>不觸發風控流程，僅更新 {@code LoanReviewDetail} 為 {@code DRAFT} 狀態。</p>
     *
     * @param id  申請識別碼
     * @param dto 行員填寫的核准條件調整資料
     * @return HTTP 200 OK
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Void>> saveReview(
            @PathVariable String id,
            @RequestBody LoanReviewDetailRequestDTO dto) {
        loanApplicationService.saveReviewDetail(id, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 正式送審，觸發風控審核流程。
     *
     * <p>將 {@code LoanReviewDetail} 狀態更新為 {@code SUBMITTED}，
     * 同步將申請狀態切換為 {@code PENDING_REVIEW}，
     * 並透過 {@code LoanRiskClient} 呼叫風控系統。</p>
     *
     * @param id 申請識別碼
     * @return HTTP 200 OK
     */
    @PatchMapping("/{id}/review/submit")
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @PathVariable String id) {
        loanApplicationService.submitReview(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 查詢指定申請的二次填單詳情。
     *
     * @param id 申請識別碼
     * @return 二次填單內容（含狀態與送審時間）
     */
    @GetMapping("/{id}/review")
    public ResponseEntity<ApiResponse<LoanReviewDetailResponseDTO>> getReview(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getReviewDetail(id)));
    }

    /**
     * 查詢最近被外部模組（風控 / 帳戶）異動過的申請。
     *
     * <p>依 {@code updateTime} 降序排列，供行員後台置頂顯示最新動態。</p>
     *
     * @return 有更新紀錄的申請清單
     */
    @GetMapping("/recent-updates")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getRecentlyUpdated() {
        return ResponseEntity.ok(ApiResponse.success(
                loanApplicationService.getRecentlyUpdated()));
    }

    /**
     * 風控送審補償：手動重新送審至風控系統。
     *
     * <p>僅允許在申請狀態為 {@code PENDING_REVIEW} 時操作，
     * 用於首次送審後風控服務未收到請求（如網路異常）的人工補救。</p>
     *
     * @param id 申請識別碼
     * @return HTTP 200 OK
     */
    @PatchMapping("/{id}/risk/retry")
    public ResponseEntity<ApiResponse<Void>> retryRiskSubmit(@PathVariable String id) {
        loanApplicationService.retryRiskSubmit(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 撥款補償：手動重新觸發撥款流程。
     *
     * <p>僅允許在申請狀態為 {@code APPROVED} 時操作，
     * 用於 {@code autoDisburse afterCommit} 回調失敗、申請卡在 {@code APPROVED} 時的人工補救。</p>
     *
     * @param id 申請識別碼
     * @return HTTP 200 OK
     */
    @PatchMapping("/{id}/disburse/retry")
    public ResponseEntity<ApiResponse<Void>> retryDisburse(@PathVariable String id) {
        loanApplicationService.retryDisburse(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
