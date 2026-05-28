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

// 行員端貸款帳戶管理 Controller
/**
 * 行員端貸款帳戶 API。
 * 提供後台依狀態查詢貸款帳戶與查看帳戶還款時間表。
 */
@RestController
@RequestMapping("/api/admin/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountAdminController {

    private final LoanAccountService   loanAccountService;
    private final LoanRepaymentService loanRepaymentService;

    // 查詢所有貸款帳戶，支援依狀態篩選
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getAllAccounts(
            @RequestParam(required = false) LoanAccountStatus status) {
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getAllAccounts(status)));
    }

    // 依申請編號查詢單筆貸款帳戶
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<LoanAccountResponseDTO>> getByApplicationId(
            @PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanAccountService.getByApplicationId(applicationId)));
    }

    // 查詢指定帳戶的完整還款時間表
    @GetMapping("/{accountId}/repayments")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> getRepayments(
            @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }

    // 手動同步指定帳戶的已繳款期數狀態
    @PostMapping("/{accountId}/repayments/sync-paid")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> syncPaidRepayment(
            @PathVariable String accountId) {
        loanRepaymentService.processRepaymentByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }
}
