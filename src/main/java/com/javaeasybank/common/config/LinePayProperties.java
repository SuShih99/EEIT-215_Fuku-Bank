package com.javaeasybank.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "linepay")
public class LinePayProperties {

    private String channelId;
    private String channelSecret;
    private String apiBase;
    private String confirmUrl;
    private String cancelUrl;
    private String returnUrl;
}
