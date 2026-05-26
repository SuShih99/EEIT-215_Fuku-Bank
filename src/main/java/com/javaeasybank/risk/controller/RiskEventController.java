package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.response.RiskEventResponse;
import com.javaeasybank.risk.service.RiskEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/risk/riskevent")
@RequiredArgsConstructor
public class RiskEventController {
    private final RiskEventService riskEventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RiskEventResponse>>> getAllRiskEvents(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RiskEventResponse> page = riskEventService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RiskEventResponse>>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actionTaken,
            @RequestParam(required = false) String riskLevel,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(riskEventService.search(eventType, actionTaken, riskLevel, pageable)));
    }
}
