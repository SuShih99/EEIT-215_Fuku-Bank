package com.javaeasybank.risk.controller;

import com.javaeasybank.risk.dto.request.RiskReviewRequestDTO;
import com.javaeasybank.risk.service.LoanReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/risk")
@RequiredArgsConstructor
public class RiskReviewController {
    private final LoanReviewService rrService;

    @PostMapping("/review")
    public ResponseEntity<Void> submitReview(
            @RequestBody RiskReviewRequestDTO request) {
        rrService.process(request);
        return ResponseEntity.accepted().build(); // 202，非同步
    }
}
