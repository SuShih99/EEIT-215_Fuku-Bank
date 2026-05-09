package com.javaeasybank.risk.controller;

import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.service.RiskEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class RiskController {

    private final RiskEventService riskEventService;

    public RiskController(RiskEventService riskEventService) {
        this.riskEventService = riskEventService;
    }

    @PostMapping("/review")
    public ResponseEntity<Void> receive(@RequestBody RiskReviewRequest request) {
        riskEventService.recordEvent(request); // 這裡會立刻回傳 200，日誌異步寫入
        return ResponseEntity.ok().build();
    }
}
