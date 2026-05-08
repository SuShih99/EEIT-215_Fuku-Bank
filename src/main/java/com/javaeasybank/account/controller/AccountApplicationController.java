package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.AccountApplicationRequest;
import com.javaeasybank.account.dto.response.AccountApplicationResponse;
import com.javaeasybank.account.enums.ApplicationStatus;
import com.javaeasybank.account.service.AccountApplicationService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.FileStorageService;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 開戶申請 Controller
 *
 * 客戶端：
 *   POST   /api/customer/account-applications          提交開戶申請（multipart）
 *   GET    /api/customer/account-applications           查詢我的申請列表
 *
 * 管理端：
 *   GET    /api/admin/account-applications              查詢全部申請（可篩選狀態）
 *   GET    /api/admin/account-applications/{id}         查詢單筆申請詳情
 *   PATCH  /api/admin/account-applications/{id}/approve 核准
 *   PATCH  /api/admin/account-applications/{id}/reject  駁回
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountApplicationController {

    private final AccountApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final JwtUtil jwtUtil;

    private static final String ID_CARDS_DIR = "id-cards";

    // =========================================================
    // 客戶端
    // =========================================================

    /**
     * 提交開戶申請（含三張證件圖片上傳）
     */
    @PostMapping(value = "/api/customer/account-applications",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> submit(
            @Valid @ModelAttribute AccountApplicationRequest request,
            @RequestParam("idFront") MultipartFile idFront,
            @RequestParam("idBack") MultipartFile idBack,
            @RequestParam("secondId") MultipartFile secondId,
            HttpServletRequest httpRequest) {

        String customerId = extractCustomerId(httpRequest);
        String applyIp = getClientIp(httpRequest);

        // 儲存三張證件圖片
        String idFrontUrl = fileStorageService.store(idFront, ID_CARDS_DIR);
        String idBackUrl = fileStorageService.store(idBack, ID_CARDS_DIR);
        String secondIdUrl = fileStorageService.store(secondId, ID_CARDS_DIR);

        AccountApplicationResponse response = applicationService.submit(
                customerId, request, idFrontUrl, idBackUrl, secondIdUrl, applyIp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("開戶申請已送出", response));
    }

    /**
     * 查詢我的開戶申請
     */
    @GetMapping("/api/customer/account-applications")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getMyApplications(
            HttpServletRequest httpRequest) {

        String customerId = extractCustomerId(httpRequest);
        List<AccountApplicationResponse> list = applicationService.getMyApplications(customerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // =========================================================
    // 管理端
    // =========================================================

    /**
     * 查詢申請列表（可選狀態篩選，分頁）
     */
    @GetMapping("/api/admin/account-applications")
    public ResponseEntity<ApiResponse<PageResponse<AccountApplicationResponse>>> listApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AccountApplicationResponse> result =
                applicationService.listByStatus(status, PageRequest.of(page, size));

        PageResponse<AccountApplicationResponse> pageResponse = PageResponse.of(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 查詢單筆申請詳情
     */
    @GetMapping("/api/admin/account-applications/{id}")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> getApplication(
            @PathVariable Long id) {

        AccountApplicationResponse response = applicationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 核准開戶申請（自動建立帳戶）
     */
    @PatchMapping("/api/admin/account-applications/{id}/approve")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> approve(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        // 管理員身分從 session / security context 取得
        String reviewedBy = getAdminUsername(httpRequest);
        AccountApplicationResponse response = applicationService.approve(id, reviewedBy);
        return ResponseEntity.ok(ApiResponse.success("開戶申請已核准", response));
    }

    /**
     * 要求補件
     */
    @PatchMapping("/api/admin/account-applications/{id}/supplement")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> requestSupplement(
            @PathVariable Long id,
            @RequestBody RejectRequest rejectRequest,
            HttpServletRequest httpRequest) {

        String reviewedBy = getAdminUsername(httpRequest);
        AccountApplicationResponse response =
                applicationService.requestSupplement(id, rejectRequest.reason(), reviewedBy);
        return ResponseEntity.ok(ApiResponse.success("已通知客戶補件", response));
    }

    /**
     * 駁回開戶申請
     */
    @PatchMapping("/api/admin/account-applications/{id}/reject")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> reject(
            @PathVariable Long id,
            @RequestBody RejectRequest rejectRequest,
            HttpServletRequest httpRequest) {

        String reviewedBy = getAdminUsername(httpRequest);
        AccountApplicationResponse response =
                applicationService.reject(id, rejectRequest.reason(), reviewedBy);
        return ResponseEntity.ok(ApiResponse.success("開戶申請已駁回", response));
    }

    // =========================================================
    // Inner DTO
    // =========================================================

    public record RejectRequest(String reason) {}

    // =========================================================
    // Private Helpers
    // =========================================================

    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }

    private String getAdminUsername(HttpServletRequest request) {
        // 從 Security Context 取得管理員帳號
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "system";
    }

    private String getClientIp(HttpServletRequest request) {
        // 優先取 X-Forwarded-For（經過反向代理時）
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
