package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.enums.BlacklistType;
import com.javaeasybank.risk.dto.request.BlackListRequest;
import com.javaeasybank.risk.dto.response.BlackListResponse;
import com.javaeasybank.risk.service.BlackListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/risk/blacklist")
public class BlackListController {

    private final BlackListService blackListService;

    //查全部
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BlackListResponse>>> getBlackLists(
            @RequestParam(required = false) Boolean activated,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(blackListService.getBlackLists(activated, pageable)));
    }

    //手動新增
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BlackListRequest>> createBlackList(@RequestBody BlackListRequest request) {
        BlackListRequest create = blackListService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(create));
    }

    /**
     * 根據業務主鍵 (類型+數值) 查詢
     */
    @GetMapping("/{type}/{value}")
    public ResponseEntity<ApiResponse<BlackListResponse>> getByBusinessKey(
            @PathVariable BlacklistType type,
            @PathVariable String value) {
        BlackListResponse response = blackListService.findByBusinessKey(type, value);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 變更黑名單狀態
     */
    @PutMapping("/{type}/{value}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable BlacklistType type,
            @PathVariable String value,
            @RequestParam Boolean status) {
        blackListService.updateStatusByBusinessKey(type, value, status);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    //更新黑名單資料
    @PostMapping("/{type}/{value}/update")
    public ResponseEntity<ApiResponse<BlackListResponse>> updateBlackList(
            @PathVariable BlacklistType type,
            @PathVariable String value,
            @RequestBody BlackListRequest request) {
        BlackListResponse updatedResponse = blackListService.updateByBusinessKey(type, value, request);
        return ResponseEntity.ok(ApiResponse.success(updatedResponse));
    }

    /**
     * 單一快速校驗（給外部模組呼叫）
     * GET /api/risk/blacklist/check?type=ACCOUNT_NO&value=帳號
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> check(
            @RequestParam BlacklistType type,
            @RequestParam String value) {

        boolean hit = blackListService.isBlacklisted(type, value);

        Map<String, Object> result = new HashMap<>();
        result.put("blacklisted", hit);
        result.put("type", type);
        result.put("value", value);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 批次校驗（給送審前預檢用）
     * POST /api/risk/blacklist/check-batch
     * Body: { "ACCOUNT_NO": "123456", "EMAIL": "xx@gmail.com" }
     */
    @PostMapping("/check-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkBatch(
            @RequestBody Map<BlacklistType, String> map) {

        List<BlacklistType> hitTypes = blackListService.checkAll(map);

        Map<String, Object> result = new HashMap<>();
        result.put("blacklisted", !hitTypes.isEmpty());
        result.put("hitTypes", hitTypes);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
