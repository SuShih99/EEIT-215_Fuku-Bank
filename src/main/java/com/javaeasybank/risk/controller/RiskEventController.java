package com.javaeasybank.risk.controller;

import com.javaeasybank.risk.dto.RiskEventResponse;
import com.javaeasybank.risk.service.RiskEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/riskevent")
@RequiredArgsConstructor
public class RiskEventController {
    private final RiskEventService riskEventService;

    @GetMapping
    public ResponseEntity<Page<RiskEventResponse>> getAllRiskEvents(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RiskEventResponse> page = riskEventService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<RiskEventResponse>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actionTaken,
            @RequestParam(required = false) String riskLevel,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // 呼叫 Service 進行查詢
        return ResponseEntity.ok(riskEventService.search(eventType, actionTaken, riskLevel, pageable));
    }
}
