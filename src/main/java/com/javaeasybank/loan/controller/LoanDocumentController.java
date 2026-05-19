package com.javaeasybank.loan.controller;

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

/**
 * 補件文件 Controller，整合客戶端與行員端的文件操作。
 *
 * <p>此 Controller 未使用 {@code @RequestMapping} 前綴，
 * 各端點依角色分別使用不同路徑前綴（{@code /api/loan-documents} 及 {@code /api/admin/loan-documents}）。</p>
 *
 * <p><b>客戶端端點：</b></p>
 * <ul>
 *   <li>{@code POST   /api/loan-documents/{applicationId}/upload} — 上傳補件文件</li>
 *   <li>{@code POST   /api/loan-documents/{applicationId}/submit} — 送出補件（標記備妥）</li>
 *   <li>{@code DELETE /api/loan-documents/{documentId}}           — 刪除自己上傳的文件</li>
 *   <li>{@code GET    /api/loan-documents/{applicationId}}        — 查詢自己申請的文件清單</li>
 *   <li>{@code GET    /api/loan-documents/types}                  — 取得文件類型下拉清單</li>
 * </ul>
 *
 * <p><b>行員端端點：</b></p>
 * <ul>
 *   <li>{@code GET    /api/admin/loan-documents/{applicationId}}  — 查任意申請的文件清單</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class LoanDocumentController {

    private final LoanDocumentService loanDocumentService;
    private final JwtUtil jwtUtil;

    // ── 客戶端 ────────────────────────────────────────────────────────

    /**
     * 上傳補件文件（{@code multipart/form-data}）。
     *
     * <p>前端以 FormData 格式送出，必填欄位：</p>
     * <ul>
     *   <li>{@code documentType} — 文件類型（{@code LoanDocumentType} 的 name()）</li>
     *   <li>{@code file}         — 文件檔案（二進位資料）</li>
     * </ul>
     * <p>Service 層會檢查文件數量上限，並在上傳後通知風控系統（best-effort）。</p>
     *
     * @param applicationId 申請識別碼
     * @param documentType  文件類型字串，例如 {@code "ID_CARD"}
     * @param file          上傳的文件檔案
     * @param request       HTTP 請求（用於解析 JWT customerId）
     * @return 上傳後的文件資訊 DTO
     */
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

    /**
     * 客戶送出補件（標記補件已全數備妥）。
     *
     * <p>寫入 {@code LoanApplication.documentsSubmittedAt} 時間戳記，
     * 行員後台才可查看並進行審核流程。</p>
     *
     * @param applicationId 申請識別碼
     * @param request       HTTP 請求（用於解析 JWT customerId）
     * @return HTTP 200 OK
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/loan-documents/{applicationId}/submit")
    public ResponseEntity<ApiResponse<Void>> submitDocs(
            @PathVariable String applicationId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        loanDocumentService.submitDocuments(applicationId, customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 刪除客戶自己上傳的補件文件。
     *
     * <p>Service 層驗證文件的 {@code uploadedBy} 必須與 JWT {@code customerId} 一致，
     * 防止刪除他人文件。</p>
     *
     * @param documentId 文件識別碼
     * @param request    HTTP 請求（用於解析 JWT customerId）
     * @return HTTP 200 OK
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/api/loan-documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDoc(
            @PathVariable String documentId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        loanDocumentService.delete(documentId, customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 查詢客戶自己申請的補件文件清單。
     *
     * <p>Service 層驗證 {@code applicationId} 所屬客戶與 JWT 一致。</p>
     *
     * @param applicationId 申請識別碼
     * @param request       HTTP 請求（用於解析 JWT customerId）
     * @return 文件清單，依上傳時間升序排列
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/loan-documents/{applicationId}")
    public ResponseEntity<ApiResponse<List<LoanDocumentResponseDTO>>> getMyDocs(
            @PathVariable String applicationId,
            HttpServletRequest request) {

        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(
                loanDocumentService.getByApplicationId(applicationId, customerId)));
    }

    // ── 行員端 ────────────────────────────────────────────────────────

    /**
     * 查詢任意申請的補件文件清單（行員端）。
     *
     * <p>無所有權限制，行員可查看任何申請的文件，供審核補件時使用。</p>
     *
     * @param applicationId 申請識別碼
     * @return 文件清單，依上傳時間升序排列
     */
    @GetMapping("/api/admin/loan-documents/{applicationId}")
    public ResponseEntity<ApiResponse<List<LoanDocumentResponseDTO>>> getAdminDocs(
            @PathVariable String applicationId) {

        return ResponseEntity.ok(ApiResponse.success(
                loanDocumentService.getByApplicationId(applicationId)));
    }

    // ── 共用 ─────────────────────────────────────────────────────────

    /**
     * 取得所有可用的文件類型及其中文名稱（客戶端 / 行員端共用）。
     *
     * <p>供前端表單動態產生文件類型下拉選單，避免前端寫死列舉值。</p>
     *
     * @return 文件類型 Map（key 為 {@code LoanDocumentType} name，value 為中文標籤）
     */
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
            return jwtUtil.getCustomerIdFromToken(authHeader.substring(7));
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
