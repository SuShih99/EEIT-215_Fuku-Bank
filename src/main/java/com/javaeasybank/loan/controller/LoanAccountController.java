package com.javaeasybank.loan.controller;

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
 * 客戶端貸款帳戶查詢 Controller。
 *
 * <p>提供客戶查詢自身貸款帳戶及還款時間表的 REST API，
 * 所有端點均需 CUSTOMER 角色並驗證帳戶所有權，防止越權存取。</p>
 *
 * <p>Base URL：{@code /api/loan-accounts}</p>
 * <ul>
 *   <li>{@code GET /my}                          — 查詢自己的所有貸款帳戶</li>
 *   <li>{@code GET /application/{applicationId}} — 依申請編號查單筆帳戶（含所有權驗證）</li>
 *   <li>{@code GET /{accountId}/repayments}       — 查詢指定帳戶的還款時間表（含所有權驗證）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountController {

    private final LoanAccountService   loanAccountService;
    private final LoanRepaymentService loanRepaymentService;
    private final JwtUtil jwtUtil;

    /**
     * 查詢客戶自己的所有貸款帳戶。
     *
     * <p>依 JWT 中的 {@code customerId} 查詢，依建立時間降序排列。</p>
     *
     * @param request HTTP 請求（用於解析 JWT）
     * @return 客戶的貸款帳戶清單
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LoanAccountResponseDTO>>> getMyAccounts(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(loanAccountService.getMyAccounts(customerId)));
    }

    /**
     * 依申請編號查詢單筆貸款帳戶，供客戶確認撥款結果時使用。
     *
     * <p>查詢後驗證帳戶的 {@code customerId} 必須與 JWT 中的值一致，
     * 防止客戶查詢他人帳戶（越權存取）。</p>
     *
     * @param applicationId 貸款申請識別碼
     * @param request       HTTP 請求（用於解析 JWT）
     * @return 對應的貸款帳戶資訊
     * @throws BusinessException 若帳戶不屬於目前登入的客戶
     */
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

    /**
     * 查詢指定貸款帳戶的完整還款時間表。
     *
     * <p>先驗證帳戶所有權（{@code customerId} 與 JWT 一致），
     * 再委派 {@code LoanRepaymentService} 取得各期還款明細，依期數升序排列。</p>
     *
     * @param accountId 貸款帳戶識別碼
     * @param request   HTTP 請求（用於解析 JWT）
     * @return 各期還款明細清單
     * @throws BusinessException 若帳戶不屬於目前登入的客戶
     */
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

    /**
     * 從 {@code Authorization} Header 解析 JWT 並取得 {@code customerId}。
     * 與 {@code LoanApplicationController} 同規格。
     *
     * @param request HTTP 請求
     * @return JWT 中的 customerId
     * @throws BusinessException 若 Header 不存在或格式不正確
     */
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
