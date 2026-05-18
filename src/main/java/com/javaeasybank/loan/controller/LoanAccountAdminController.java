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

/*
 * 行員端：貸款帳戶管理入口
 *   GET /api/admin/loan-accounts                        - 查全部（可依 status 篩選）
 *   GET /api/admin/loan-accounts/application/{id}       - 依申請編號查帳戶
 *   GET /api/admin/loan-accounts/{accountId}/repayments - 查還款時間表
 */
@RestController
@RequestMapping("/api/admin/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountAdminController {

    private final LoanAccountService    loanAccountService;
    private final LoanRepaymentService  loanRepaymentService;

    // 查全部帳戶（不帶 status 參數則顯示全部）
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getAllAccounts(
            @RequestParam(required = false) LoanAccountStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getAllAccounts(status)));
    }

    // 依申請編號查單筆帳戶
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<LoanAccountResponseDTO>> getByApplicationId(
            @PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanAccountService.getByApplicationId(applicationId)));
    }

    // 查某帳戶的完整還款時間表
    @GetMapping("/{accountId}/repayments")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> getRepayments(
            @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }

    @PostMapping("/{accountId}/repayments/sync-paid")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> syncPaidRepayment(
            @PathVariable String accountId) {
        loanRepaymentService.processRepaymentByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }
}
