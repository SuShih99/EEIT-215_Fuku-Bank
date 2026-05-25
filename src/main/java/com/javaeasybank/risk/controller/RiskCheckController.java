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



    private final RiskCheckService riskCheckService;

    @PostMapping
    public ResponseEntity<ApiResponse<RiskCheckResponse>> checkRisk(
            @Valid @RequestBody RiskCheckRequest request) {
        RiskCheckResponse result = riskCheckService.check(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
