package com.javaeasybank.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
@Configuration
public class DateTimeConfig {

    @Bean
    public Clock clock() {
        // 台灣開發通常使用台北時區
        return Clock.system(ZoneId.of("Asia/Taipei"));
    }
}
