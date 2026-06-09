package com.javaeasybank.loan.client;

/**
 * 程式說明：
 * - 對外呼叫風險評分服務的 HTTP Client。
 * - 將貸款申請資料送至風控模組，並把回傳結果提供給審核流程使用。
 */

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.requests.LoanDocumentInfoDTO;
import com.javaeasybank.loan.dto.requests.LoanRiskRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// 風控模組 HTTP 客戶端
@Slf4j
/**
 * 風控模組整合 Client。
 * 封裝送審、補件回補與連線失敗時的錯誤處理，讓貸款服務層不直接依賴 HTTP 細節。
 */
@Component
public class LoanRiskClient {

    // 風控 API 的 base URL，例如 http://risk-service/api/risk/
    @Value("${risk.api.base-url}")
    private String riskBaseUrl;

    // 風控完成審核後回呼的 base URL
    @Value("${risk.api.loan.callback-url:${risk.api.callback-url:http://localhost:8080/api/loan-callbacks}}")
    private String callbackBaseUrl;

    private final RestTemplate restTemplate;

    public LoanRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 補件通知：將客戶新上傳的文件清單推送給風控，更新對應的審核任務（ReviewTask）
    public void attachDocuments(String businessId, List<LoanDocumentInfoDTO> documents) {
        String url = buildRiskUrl("reviews/" + businessId + "/attachments");
        log.info("[RiskClient] 補件通知 businessId={} count={} → {}", businessId, documents.size(), url);
        try {
            Map<String, Object> body = Map.of("documents", documents);
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), Void.class);
        } catch (RestClientException e) {
            // 補件通知失敗不影響主流程，僅記錄警告
            log.warn("[RiskClient] 補件通知風控失敗 businessId={} err={}", businessId, e.getMessage());
        }
    }

    // 送審：將整合後的審核資料 POST 至風控系統的 /risk/reviews 入口
    public void submitForReview(LoanRiskRequestDTO dto) {
        dto.setCallbackUrl(callbackBaseUrl + "/" + dto.getApplicationId() + "/status");

        String url = buildRiskUrl("reviews");
        log.info("[RiskClient] 送審 applicationId={} → {}", dto.getApplicationId(), url);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, dto, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException("風控模組回應異常，HTTP " + response.getStatusCode().value());
            }
        } catch (RestClientException e) {
            log.error("[RiskClient] 呼叫風控失敗 applicationId={}", dto.getApplicationId(), e);
            throw new BusinessException("送審失敗：無法連接風控模組，請稍後再試");
        }
    }

    // 組裝完整的風控 API URL
    private String buildRiskUrl(String path) {
        String normalizedBaseUrl = riskBaseUrl.endsWith("/") ? riskBaseUrl : riskBaseUrl + "/";
        return normalizedBaseUrl + path;
    }
}
