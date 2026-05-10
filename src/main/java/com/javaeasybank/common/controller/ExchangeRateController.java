package com.javaeasybank.common.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExchangeRates() {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getLatestRates()));
    }
}
