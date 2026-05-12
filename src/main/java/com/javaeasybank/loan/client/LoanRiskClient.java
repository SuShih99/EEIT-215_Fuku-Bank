package com.javaeasybank.loan.client;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.requests.LoanRiskRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/*
 * 負責將送審資料傳給風控模組。
 * 使用 RestTemplate 發 POST；若風控服務不可達，丟出 BusinessException
 * 讓 @Transactional 回滾整筆 submitReview 操作，避免主表停在 PENDING_REVIEW 卻沒有真正送出。
 */
@Slf4j
@Component
public class LoanRiskClient {

    // application.properties:
    //   risk.api.base-url=http://localhost:8080/api/risk/reviews
    //   risk.api.callback-url=http://localhost:8080/api/loan-callbacks
    @Value("@/api/risk/reviews")
    private String riskBaseUrl;

    @Value("@/api/loan-callbacks")
    private String callbackBaseUrl;

    private final RestTemplate restTemplate;

    public LoanRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 送審：把 DTO 打到風控的 /risk/review 入口
    public void submitForReview(LoanRiskRequestDTO dto) {

        dto.setCallbackUrl(callbackBaseUrl + "/" + dto.getApplicationId() + "/status");

        String url = riskBaseUrl;
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