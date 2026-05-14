package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.RiskCheckRequest;
import com.javaeasybank.risk.dto.response.RiskCheckResponse;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.service.RiskCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/risk/check")
@RequiredArgsConstructor
public class RiskCheckController {

    /**
     * 交易風險檢查。
     * 在特定業務流程（如轉帳、登入、修改資料）執行前呼叫，用於實時判斷潛在風險。
     * 這類 API 屬於「同步檢查」，不產生人工審核任務，僅回傳風險評分或等級。
     * 1. 執行實時風控規則判斷。
     * 2. 記錄所有風險事件至 RISK_EVENT_LOG。
     * 3. 若判定為 MANUAL_REVIEW，則建立人工審核任務 (ReviewTask)。
     */

    private final RiskCheckService riskCheckService;

    @PostMapping
    public ResponseEntity<ApiResponse<RiskCheckResponse>> checkRisk(
            @Valid @RequestBody RiskCheckRequest request) {
        RiskCheckResponse result = riskCheckService.check(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
