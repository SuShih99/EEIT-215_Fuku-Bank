package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.loan.dto.response.LoanAccountResponseDTO;
import com.javaeasybank.loan.dto.response.LoanRepaymentResponseDTO;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.service.LoanAccountService;
import com.javaeasybank.loan.service.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行員端貸款帳戶管理 Controller。
 *
 * <p>提供行員查詢貸款帳戶、還款時間表及手動同步繳款狀態的 REST API，
 * 無需身分驗證（由 Spring Security 在路由層統一管控行員路徑存取）。</p>
 *
 * <p>Base URL：{@code /api/admin/loan-accounts}</p>
 * <ul>
 *   <li>{@code GET  /}                              — 查全部帳戶（可依 status 篩選）</li>
 *   <li>{@code GET  /application/{applicationId}}   — 依申請編號查單筆帳戶</li>
 *   <li>{@code GET  /{accountId}/repayments}         — 查指定帳戶的完整還款時間表</li>
 *   <li>{@code POST /{accountId}/repayments/sync-paid} — 手動同步已繳款期數狀態</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountAdminController {

    private final LoanAccountService   loanAccountService;
    private final LoanRepaymentService loanRepaymentService;

    /**
     * 查詢所有貸款帳戶，支援依狀態篩選。
     *
     * <p>不帶 {@code status} 參數時回傳全部帳戶，
     * 供行員後台管理頁面使用。</p>
     *
     * @param status 帳戶狀態篩選條件（選填）；不帶時顯示全部，參見 {@code LoanAccountStatus}
     * @return 符合條件的貸款帳戶清單
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getAllAccounts(
            @RequestParam(required = false) LoanAccountStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getAllAccounts(status)));
    }

    /**
     * 依申請編號查詢單筆貸款帳戶。
     *
     * <p>行員在申請詳情頁確認撥款結果時使用，無需額外所有權驗證。</p>
     *
     * @param applicationId 貸款申請識別碼
     * @return 對應的貸款帳戶資訊
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<LoanAccountResponseDTO>> getByApplicationId(
            @PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanAccountService.getByApplicationId(applicationId)));
    }

    /**
     * 查詢指定帳戶的完整還款時間表。
     *
     * @param accountId 貸款帳戶識別碼
     * @return 各期還款明細清單，依期數升序排列
     */
    @GetMapping("/{accountId}/repayments")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> getRepayments(
            @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }

    /**
     * 手動同步指定帳戶的已繳款期數狀態。
     *
     * <p>當帳戶模組通知繳款成功後，由此端點觸發 {@code LoanRepaymentService}
     * 掃描並更新已繳清的期數狀態，回傳同步後的最新還款明細。</p>
     *
     * @param accountId 貸款帳戶識別碼
     * @return 同步後的各期還款明細清單
     */
    @PostMapping("/{accountId}/repayments/sync-paid")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> syncPaidRepayment(
            @PathVariable String accountId) {
        loanRepaymentService.processRepaymentByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }
}
