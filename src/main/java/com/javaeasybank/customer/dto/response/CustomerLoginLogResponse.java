package com.javaeasybank.customer.dto.response;

import com.javaeasybank.customer.entity.CustomerLoginLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerLoginLogResponse {
    private Long loginLogId;
    private String username;
    private String result;
    private String failReason;
    private String ipAddress;
    private String deviceName;
    private LocalDateTime loginTime;

    public static CustomerLoginLogResponse fromEntity(CustomerLoginLog log) {
        CustomerLoginLogResponse response = new CustomerLoginLogResponse();
        response.setLoginLogId(log.getLoginLogId());
        response.setUsername(log.getUsername());
        response.setResult(log.getResult());
        response.setFailReason(log.getFailReason());
        response.setIpAddress(log.getIpAddress());
        response.setDeviceName(log.getDeviceName());
        response.setLoginTime(log.getLoginTime());
        return response;
    }
}
