package com.javaeasybank.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 使用 Apache HttpClient 5 作為底層，完美支援 PATCH
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        // 可選：設定連線逾時時間（毫秒）
        factory.setConnectionRequestTimeout(5000);

        return new RestTemplate(factory);
    }

}
