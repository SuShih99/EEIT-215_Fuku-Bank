package com.javaeasybank.loan.controller;

/**
 * 程式說明：
 * - 會員端貸款帳戶 API。
 * - 提供登入會員查詢自己的貸款帳戶、還款明細與帳務狀態。
 */

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.loan.dto.response.LoanAccountResponseDTO;
import com.javaeasybank.loan.dto.response.LoanRepaymentResponseDTO;
import com.javaeasybank.loan.service.LoanAccountService;
import com.javaeasybank.loan.service.LoanRepaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客戶端貸款帳戶 API。
 * 提供會員查詢自己的貸款帳戶、還款明細與送出還款交易。
 */
@RestController
@RequestMapping("/api/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountController {

    private final LoanAccountService   loanAccountService;
    private final LoanRepaymentService loanRepaymentService;
    private final JwtUtil jwtUtil;

    // 查詢客戶自己的所有貸款帳戶
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getMyAccounts(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getMyAccounts(customerId)));
    }

    // 依申請編號查詢單筆貸款帳戶，供客戶確認撥款結果時使用
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<LoanAccountResponseDTO>> getByApplicationId(
            @PathVariable String applicationId,
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        LoanAccountResponseDTO dto = loanAccountService.getByApplicationId(applicationId);
        if (!customerId.equals(dto.getCustomerId())) {
            throw new BusinessException("無權存取此帳戶");
        }
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // 查詢指定貸款帳戶的完整還款時間表
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/{accountId}/repayments")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDTO>>> getRepayments(
            @PathVariable String accountId,
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        LoanAccountResponseDTO account = loanAccountService.getAccountById(accountId);
        if (!customerId.equals(account.getCustomerId())) {
            throw new BusinessException("無權存取此帳戶");
        }
        return ResponseEntity.ok(ApiResponse.success(
                loanRepaymentService.getByAccountId(accountId)));
    }

    // 從 Authorization Header 解析 JWT 並取得 customerId
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
