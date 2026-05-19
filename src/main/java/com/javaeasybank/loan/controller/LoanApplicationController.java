package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.requests.LoanMemberRequestDTO;
import com.javaeasybank.loan.dto.response.LoanApplicationResponseDTO;
import com.javaeasybank.loan.service.LoanApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客戶端貸款申請入口 Controller。
 *
 * <p>提供客戶申請貸款、查詢個人申請紀錄及取得利率規則的 REST API，
 * 所有需要身分驗證的端點均透過 JWT 解析 {@code customerId}。</p>
 *
 * <p>Base URL：{@code /api/loan-applications}</p>
 * <ul>
 *   <li>{@code POST /member}   — 提交新的貸款申請（需 CUSTOMER 角色）</li>
 *   <li>{@code GET  /my}       — 查詢自己的所有申請紀錄（需 CUSTOMER 角色）</li>
 *   <li>{@code GET  /rate-rules} — 取得各貸款種類的利率規則（不需身分驗證）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final JwtUtil jwtUtil;

    /**
     * 提交貸款申請。
     *
     * <p>從 JWT 取得 {@code customerId}，再呼叫 Service 建立申請，
     * 申請初始狀態為 {@code PENDING_CONTACT}，回傳新建的 {@code applicationId}。</p>
     *
     * @param dto     客戶填寫的申請資料
     * @param request HTTP 請求（用於解析 JWT）
     * @return HTTP 201 Created，body 為新建的 {@code applicationId}
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/member")
    public ResponseEntity<ApiResponse<String>> applyMember(
            @RequestBody LoanMemberRequestDTO dto, HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        String applicationId = loanApplicationService.insertMember(customerId, dto);
        return ResponseEntity.status(201).body(ApiResponse.success(applicationId));
    }

    /**
     * 查詢客戶自己的所有貸款申請。
     *
     * <p>依 JWT 中的 {@code customerId} 查詢，依建立時間降序排列。</p>
     *
     * @param request HTTP 請求（用於解析 JWT）
     * @return 客戶的申請清單
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getMyApplications(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(
                loanApplicationService.getMyApplications(customerId)));
    }

    /**
     * 取得各貸款種類的利率規則。
     *
     * <p>供申請表單載入時呼叫，回傳各種貸款種類對應的可選利率區間或固定利率，
     * 不需身分驗證。</p>
     *
     * @return 利率規則 Map（key 為貸款種類，value 為利率相關設定）
     */
    @GetMapping("/rate-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateRules() {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getRateRules()));
    }

    /**
     * 從 {@code Authorization} Header 解析 JWT 並取得 {@code customerId}。
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
