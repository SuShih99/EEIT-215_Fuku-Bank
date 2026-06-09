package com.javaeasybank.loan.controller;

/**
 * 程式說明：
 * - 貸款文件上傳與查詢 API。
 * - 管理會員補件、審核文件清單、合約 PDF 下載與文件狀態回報。
 */

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.loan.dto.response.LoanDocumentResponseDTO;
import com.javaeasybank.loan.service.LoanDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// 補件文件 Controller，整合客戶端與行員端的文件操作
/**
 * 貸款補件文件 API。
 * 同時提供客戶端上傳/刪除/送出補件，以及行員端查閱補件清單的功能。
 */
@RestController
@RequiredArgsConstructor
public class LoanDocumentController {

    private final LoanDocumentService loanDocumentService;
    private final JwtUtil jwtUtil;

    // 上傳補件文件（multipart/form-data）
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping(value = "/api/loan-documents/{applicationId}/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoanDocumentResponseDTO>> upload(
            @PathVariable String applicationId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        LoanDocumentResponseDTO dto =
                loanDocumentService.upload(applicationId, customerId, documentType, file);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // 客戶送出補件（標記補件已全數備妥）
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/loan-documents/{applicationId}/submit")
    public ResponseEntity<ApiResponse<Void>> submitDocs(
            @PathVariable String applicationId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        loanDocumentService.submitDocuments(applicationId, customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 刪除客戶自己上傳的補件文件
    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/api/loan-documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDoc(
            @PathVariable String documentId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        loanDocumentService.delete(documentId, customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 查詢客戶自己申請的補件文件清單
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/loan-documents/{applicationId}")
    public ResponseEntity<ApiResponse<List<LoanDocumentResponseDTO>>> getMyDocs(
            @PathVariable String applicationId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(
                loanDocumentService.getByApplicationId(applicationId, customerId)));
    }

    // 查詢任意申請的補件文件清單（行員端）
    @GetMapping("/api/admin/loan-documents/{applicationId}")
    public ResponseEntity<ApiResponse<List<LoanDocumentResponseDTO>>> getAdminDocs(
            @PathVariable String applicationId) {

        return ResponseEntity.ok(ApiResponse.success(
                loanDocumentService.getByApplicationId(applicationId)));
    }

    // 取得所有可用的文件類型及其中文名稱（客戶端 / 行員端共用）
    @GetMapping("/api/loan-documents/types")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDocumentTypes() {
        Map<String, String> types = new java.util.LinkedHashMap<>();
        types.put("ID_CARD",         "身分證");
        types.put("INCOME_CERT",     "收入證明");
        types.put("EMPLOYMENT_CERT", "在職證明");
        types.put("BANK_STATEMENT",  "銀行存摺");
        types.put("PROPERTY_CERT",   "不動產謄本");
        types.put("TITLE_DEED",      "所有權狀");
        types.put("OTHER",           "其他");
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    // 從 Authorization Header 解析 JWT 並取得 customerId
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtUtil.getCustomerIdFromToken(authHeader.substring(7));
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
