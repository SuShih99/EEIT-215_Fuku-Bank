package com.javaeasybank.customer.service;

import com.javaeasybank.customer.entity.CustomerLoginLog;
import com.javaeasybank.customer.repository.CustomerLoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final CustomerLoginLogRepository customerLoginLogRepository;

    @Async
    public void recordLogin(String customerId, String username, String result, String failReason,
            String ipAddress, String userAgent, String deviceName) {
        CustomerLoginLog log = new CustomerLoginLog();
        log.setCustomerId(customerId);
        log.setUsername(truncate(username == null || username.isBlank() ? "UNKNOWN" : username, 50));
        log.setResult(result);
        log.setFailReason(truncate(failReason, 200));
        log.setIpAddress(truncate(ipAddress, 45));
        log.setUserAgent(truncate(userAgent, 512));
        log.setDeviceName(truncate(deviceName, 120));
        customerLoginLogRepository.save(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
