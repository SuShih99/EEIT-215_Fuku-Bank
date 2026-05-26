package com.javaeasybank.customer.dto.response;

import com.javaeasybank.customer.entity.CustomerDevice;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerDeviceResponse {
    private Long deviceId;
    private String deviceName;
    private String browserName;
    private String operatingSystem;
    private String ipAddress;
    private String status;
    private Boolean trusted;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime revokedAt;

    public static CustomerDeviceResponse fromEntity(CustomerDevice device) {
        CustomerDeviceResponse response = new CustomerDeviceResponse();
        response.setDeviceId(device.getDeviceId());
        response.setDeviceName(device.getDeviceName());
        response.setBrowserName(device.getBrowserName());
        response.setOperatingSystem(device.getOperatingSystem());
        response.setIpAddress(device.getIpAddress());
        response.setStatus(device.getStatus());
        response.setTrusted(device.getTrusted());
        response.setFirstSeenAt(device.getFirstSeenAt());
        response.setLastSeenAt(device.getLastSeenAt());
        response.setRevokedAt(device.getRevokedAt());
        return response;
    }
}
