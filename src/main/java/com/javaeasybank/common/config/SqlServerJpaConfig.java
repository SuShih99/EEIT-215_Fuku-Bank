package com.javaeasybank.common.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SqlServerJpaConfig {

    @Bean
    public HibernatePropertiesCustomizer sqlServerHibernateCustomizer() {
        return properties ->
                properties.put("hibernate.jdbc.batch_versioned_data", "false");
    }
}
