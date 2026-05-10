package com.javaeasybank.customer.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.dto.response.CustomerDeviceResponse;
import com.javaeasybank.customer.dto.response.CustomerLoginLogResponse;
import com.javaeasybank.customer.entity.CustomerDevice;
import com.javaeasybank.customer.repository.CustomerDeviceRepository;
import com.javaeasybank.customer.repository.CustomerLoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerSecurityService {

    private final CustomerLoginLogRepository customerLoginLogRepository;
    private final CustomerDeviceRepository customerDeviceRepository;

    public List<CustomerLoginLogResponse> getLoginLogs(String customerId) {
        return customerLoginLogRepository.findTop30ByCustomerIdOrderByLoginTimeDesc(customerId).stream()
                .map(CustomerLoginLogResponse::fromEntity)
                .toList();
    }

    public List<CustomerDeviceResponse> getDevices(String customerId) {
        return customerDeviceRepository.findByCustomerIdOrderByLastSeenAtDesc(customerId).stream()
                .map(CustomerDeviceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void revokeDevice(String customerId, Long deviceId) {
        CustomerDevice device = customerDeviceRepository.findByDeviceIdAndCustomerId(deviceId, customerId)
                .orElseThrow(() -> new BusinessException("查無此授權裝置"));
        device.setStatus("REVOKED");
        device.setTrusted(false);
        device.setRevokedAt(LocalDateTime.now());
        customerDeviceRepository.save(device);
    }
}
