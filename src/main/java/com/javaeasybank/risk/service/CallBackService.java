package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.request.RiskCallbackRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CallBackService {
    private final RestTemplate restTemplate;

    public void sendCallback(String callbackUrl, String callerModule,
                             Long targetId, String newStatus) {

        RiskCallbackRequestDTO body = new RiskCallbackRequestDTO();
        body.setCallerModule(callerModule);
        body.setNewStatus(newStatus);

        try {
            restTemplate.postForEntity(callbackUrl, body, Void.class);
            log.info("[Callback] {} targetId={} → {}", callerModule, targetId, newStatus);
        } catch (RestClientException e) {
            log.error("[Callback] 打回失敗 callerModule={} targetId={}", callerModule, targetId, e);
            // 視情況加 retry 或寫進 dead-letter log
        }
    }
}
