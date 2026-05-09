package com.javaeasybank.common.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExchangeRates() {
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/TWD";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates", e);
            throw new RuntimeException("匯率獲取失敗");
        }
    }
}
