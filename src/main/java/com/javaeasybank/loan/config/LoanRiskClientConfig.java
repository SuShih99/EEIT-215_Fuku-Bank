package com.javaeasybank.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LoanRiskClientConfig {

    // 若專案已有全域 RestTemplate bean 可直接移除這裡，注入那個即可
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}