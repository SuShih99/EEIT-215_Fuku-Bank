package com.javaeasybank.loan.client;

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

/*
 * 負責將送審資料傳給風控模組。
 * 使用 RestTemplate 發 POST；若風控服務不可達，丟出 BusinessException
 * 讓 @Transactional 回滾整筆 submitReview 操作，避免主表停在 PENDING_REVIEW 卻沒有真正送出。
 */
@Slf4j
@Component
public class LoanRiskClient {

    // application.properties:
    //   risk.api.base-url=http://localhost:8080/api/risk/
    //   risk.api.callback-url=http://localhost:8080/api/loan-callbacks
    @Value("${risk.api.base-url}")
    private String riskBaseUrl;

    @Value("${risk.api.loan.callback-url}")
    private String callbackBaseUrl;

    private final RestTemplate restTemplate;

    public LoanRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 補件通知：把文件清單推給風控，更新對應的 ReviewTask
    public void attachDocuments(String businessId, List<LoanDocumentInfoDTO> documents) {
        String url = riskBaseUrl + "reviews/" + businessId + "/attachments";
        log.info("[RiskClient] 補件通知 businessId={} count={} → {}", businessId, documents.size(), url);
        try {
            Map<String, Object> body = Map.of("documents", documents);
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), Void.class);
        } catch (RestClientException e) {
            // 補件通知失敗不影響主流程，僅記錄警告
            log.warn("[RiskClient] 補件通知風控失敗 businessId={} err={}", businessId, e.getMessage());
        }
    }

    // 送審：把 DTO 打到風控的 /risk/review 入口
    public void submitForReview(LoanRiskRequestDTO dto) {

        dto.setCallbackUrl(callbackBaseUrl + "/" + dto.getApplicationId() + "/status");

        String url = riskBaseUrl + "reviews";
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
}