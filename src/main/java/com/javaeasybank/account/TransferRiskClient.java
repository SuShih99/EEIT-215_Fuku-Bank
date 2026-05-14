package com.javaeasybank.account;

import com.javaeasybank.account.exception.TransferException;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.RiskCheckRequest;
import com.javaeasybank.risk.dto.response.RiskCheckResponse;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.enums.Disposition;
import com.javaeasybank.risk.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class TransferRiskClient {
    @Value("${risk.api.base-url}")
    private String riskBaseUrl;

    @Value("${risk.api.transfer-callback-url}")
    private String callbackBaseUrl;

    private final RestTemplate restTemplate;

    public TransferRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RiskCheckResponse checkTransfer(
            String customerId, String toAccNum,
            BigDecimal amount, String referenceId) {

        RiskCheckRequest req = new RiskCheckRequest();
        req.setCustomerId(customerId);
        req.setScene(BusinessScene.TRANSFER_REVIEW);
        req.setBusinessId(referenceId);
        req.setAmount(amount);
        req.setTargetIdentifier(toAccNum);
        req.setCallbackUrl(callbackBaseUrl + "/" + referenceId + "/status");

        String url = riskBaseUrl + "check";
        try {
            ResponseEntity<ApiResponse> res =
                    restTemplate.postForEntity(url, req, ApiResponse.class);
            // 解析回傳
            Map<?, ?> data = (Map<?, ?>) res.getBody().getData();
            return parseCheckResponse(data);

        } catch (RestClientException e) {
            log.error("[RiskClient] 風控檢查失敗", e);
            if (amount.compareTo(new BigDecimal("50000")) >= 0) {
                throw new TransferException("RISK_SERVICE_UNAVAILABLE",
                        "風控服務異常，大額交易暫停");
            }
            return RiskCheckResponse.builder()
                    .disposition(Disposition.PASS)
                    .riskLevel(RiskLevel.LOW)
                    .reason("風控降級，小額放行")
                    .build();
        }
    }

    private RiskCheckResponse parseCheckResponse(Map<?, ?> data) {
        String disposition = String.valueOf(data.get("disposition"));
        String riskLevel = String.valueOf(data.get("riskLevel"));
        String reason = String.valueOf(data.get("reason"));
        Object logIdObj = data.get("logId");
        Object taskIdObj = data.get("reviewTaskId");

        return RiskCheckResponse.builder()
                .disposition(Disposition.valueOf(disposition))
                .riskLevel(RiskLevel.valueOf(riskLevel))
                .reason(reason)
                .logId(logIdObj != null
                        ? Long.valueOf(logIdObj.toString()) : null)
                .reviewTaskId(taskIdObj != null
                        ? Long.valueOf(taskIdObj.toString()) : null)
                .build();
    }
}
