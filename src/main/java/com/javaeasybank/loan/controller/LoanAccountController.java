package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.loan.dto.response.LoanAccountResponseDTO;
import com.javaeasybank.loan.service.LoanAccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 客戶端：貸款帳戶查詢入口
 *   GET /api/loan-accounts/my                       - 查自己所有帳戶
 *   GET /api/loan-accounts/application/{id}         - 依申請編號查單筆帳戶
 */
@RestController
@RequestMapping("/api/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountController {

    private final LoanAccountService loanAccountService;
    private final JwtUtil jwtUtil;

    // 查詢自己的所有貸款帳戶
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getMyAccounts(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getMyAccounts(customerId)));
    }

    // 依申請編號查單筆帳戶（客戶確認撥款結果時使用）
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<LoanAccountResponseDTO>> getByApplicationId(
            @PathVariable String applicationId,
            HttpServletRequest request) {
        extractCustomerId(request); // 確保是已登入的客戶才能呼叫
        return ResponseEntity.ok(ApiResponse.success(
                loanAccountService.getByApplicationId(applicationId)));
    }

    // Helper：從 Authorization Header 解析 customerId，與 LoanApplicationController 同規格
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
