package com.javaeasybank.auth.controller;

import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.service.AuthActionLogService;
import com.javaeasybank.common.dto.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/logs")
public class AuthActionLogController {

    private final AuthActionLogService actionLogService;

    public AuthActionLogController(AuthActionLogService actionLogService) {
        this.actionLogService = actionLogService;
    }

    /** 查詢所有日誌：僅 CISO (permLevel >= 4) 可存取 */
    @GetMapping
    @PreAuthorize("hasRole('CISO')")
    public ApiResponse<List<AuthActionLog>> getAllLogs() {
        return ApiResponse.success(actionLogService.getAllLogs());
    }

    /** 匯出 CSV：僅 CISO 可存取 */
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('CISO')")
    public ResponseEntity<byte[]> exportCsv() throws Exception {
        byte[] data = actionLogService.exportToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system_logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    /** 匯出 PDF：僅 CISO 可存取 */
    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('CISO')")
    public ResponseEntity<byte[]> exportPdf() throws Exception {
        byte[] data = actionLogService.exportToPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system_logs.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
