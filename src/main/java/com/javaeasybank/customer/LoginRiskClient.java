package com.javaeasybank.customer;

import com.javaeasybank.risk.dto.request.RiskCheckRequest;
import com.javaeasybank.risk.enums.BusinessScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class LoginRiskClient {
    @Value("${risk.api.base-url}")
    private String riskBaseUrl;

    private final RestTemplate restTemplate;

    public LoginRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void recordLoginEvent(
            String customerId, String ipAddress,
            String eventType, String reason) {

        RiskCheckRequest req = new RiskCheckRequest();
        req.setCustomerId(customerId);
        req.setScene(BusinessScene.LOGIN_CHECK);
        req.setBusinessId(ipAddress);
        req.setTargetIdentifier("客戶ID: " + customerId);
        req.addContext("eventType", eventType);
        req.addContext("reason", reason);

        String url = buildRiskUrl();
        log.info("[LoginRiskClient] 嘗試發送風控事件 customerId={}, eventType={}, url={}",
                customerId, eventType, url);

        try {
            restTemplate.postForEntity(url, req, Void.class);
        } catch (org.springframework.web.client.HttpStatusCodeException ignored) {
        } catch (RestClientException e) {
            log.error("[LoginRiskClient] 網路連線失敗或目標伺服器未啟動", e);
        }
    }

    private String buildRiskUrl() {
        String normalizedBaseUrl = riskBaseUrl.endsWith("/") ? riskBaseUrl : riskBaseUrl + "/";
        return normalizedBaseUrl + "check";
    }
}
