package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.loan.dto.requests.LoanStatusCallbackRequestDTO;
import com.javaeasybank.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 外部模組回調 Controller。
 *
 * <p>接收其他微服務審核或處理完成後的狀態更新通知，
 * 統一透過 {@code callerModule} 欄位分流至對應的業務邏輯。</p>
 *
 * <p>目前支援的回調來源：</p>
 * <ul>
 *   <li><b>風控模組（RISK）</b>：審核完成後將申請狀態從
 *       {@code PENDING_REVIEW} 更新為 {@code APPROVED} 或 {@code REJECTED}。</li>
 *   <li><b>帳戶模組（ACCOUNT）</b>：撥款完成後將申請狀態從
 *       {@code APPROVED} 更新為 {@code DISBURSED}，並帶回貸款帳號。</li>
 * </ul>
 *
 * <p>Base URL：{@code /api/loan-callbacks}</p>
 */
@RestController
@RequestMapping("/api/loan-callbacks")
@RequiredArgsConstructor
@Slf4j
public class LoanCallbackController {

    private final LoanApplicationService loanApplicationService;

    /**
     * 接收外部模組的申請狀態更新回調。
     *
     * <p>由 {@code LoanRiskClient} 送審時注入的 {@code callbackUrl} 指向此端點。
     * 回調內容包含目標狀態、呼叫方識別及備註，
     * 委派 {@code LoanApplicationService.handleStatusCallback} 執行狀態轉移邏輯。</p>
     *
     * <p>請求範例：</p>
     * <pre>
     * POST /api/loan-callbacks/{applicationId}/status
     * {
     *   "newStatus": "APPROVED",
     *   "callerModule": "RISK",
     *   "note": "信用評分通過"
     * }
     * </pre>
     *
     * @param applicationId 目標貸款申請識別碼
     * @param dto           回調資料，包含目標狀態與呼叫方資訊
     * @return HTTP 200 OK
     */
    @PostMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<Void>> handleStatusCallback(
            @PathVariable String applicationId,
            @RequestBody LoanStatusCallbackRequestDTO dto) {

        log.info("[Callback] 收到回調 applicationId={} newStatus={} callerModule={} note={}",
                applicationId, dto.getNewStatus(), dto.getCallerModule(), dto.getNote());

        loanApplicationService.handleStatusCallback(applicationId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
