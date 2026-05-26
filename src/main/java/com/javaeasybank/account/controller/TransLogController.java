package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.service.TransLogService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易紀錄操作的 REST 控制器。
 * 處理來自客戶端的交易紀錄查詢相關請求。
 */
@Slf4j
@RestController
@RequestMapping("/api/trans-logs")
@RequiredArgsConstructor
public class TransLogController {

    private final TransLogService transLogService;

    /**
     * 根據交易參考 ID 檢索交易紀錄。
     *
     * @param referenceId 交易參考 ID。
     * @return 包含交易紀錄響應列表的 ResponseEntity。
     */
    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<ApiResponse<List<TransLogResponse>>> getByReferenceId(@PathVariable String referenceId) {
        log.info("Received request to get trans logs by referenceId: {}", referenceId);
        List<TransLogResponse> responses = transLogService.getByReferenceId(referenceId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }


    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getByAccountNumber(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get trans logs by accountNumber: {}", accountNumber);
        Page<TransLogResponse> transLogPage = transLogService.getByAccountNumber(accountNumber, PageRequest.of(page, size));
        PageResponse<TransLogResponse> pageResponse = PageResponse.of(
                transLogPage.getContent(),
                transLogPage.getNumber(),
                transLogPage.getSize(),
                transLogPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }


    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getByCustomerId(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get trans logs by customerId: {}", customerId);
        Page<TransLogResponse> transLogPage = transLogService.getByCustomerId(customerId, PageRequest.of(page, size));
        PageResponse<TransLogResponse> pageResponse = PageResponse.of(
                transLogPage.getContent(),
                transLogPage.getNumber(),
                transLogPage.getSize(),
                transLogPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }


    @GetMapping("/customer/{customerId}/range")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getByCustomerIdAndDateRange(
            @PathVariable String customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get trans logs by customerId: {} and date range: {} to {}", customerId, startDate, endDate);
        Page<TransLogResponse> transLogPage = transLogService.getByCustomerIdAndDateRange(
                customerId, startDate, endDate, PageRequest.of(page, size));
        PageResponse<TransLogResponse> pageResponse = PageResponse.of(
                transLogPage.getContent(),
                transLogPage.getNumber(),
                transLogPage.getSize(),
                transLogPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 檢索最新建立的交易紀錄分頁列表。
     *
     * @param page 頁碼（默認為0）。
     * @param size 每頁紀錄數量（默認為10）。
     * @return 包含最新交易紀錄分頁列表響應的 ResponseEntity。
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getLatestTransLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get latest trans logs");
        Page<TransLogResponse> transLogPage = transLogService.getLatest(PageRequest.of(page, size));
        PageResponse<TransLogResponse> pageResponse = PageResponse.of(
                transLogPage.getContent(),
                transLogPage.getNumber(),
                transLogPage.getSize(),
                transLogPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 查詢全站交易紀錄統計數據
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(transLogService.getStatistics()));
    }
}