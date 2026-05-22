package com.javaeasybank.common.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SQL Server + Hibernate 7 相容性設定。
 *
 * 設定 Hibernate 屬性以避免 SQL Server JDBC Driver 回傳 -1 row count
 * 導致的 ObjectOptimisticLockingFailureException。
 *
 * 主要修正在 JDBC URL 層（lastUpdateCount=true, useBulkCopyForBatchInsert=false），
 * 此處僅補充 Hibernate batch 層的保護。
 */
@Configuration
public class SqlServerJpaConfig {

    @Bean
    public HibernatePropertiesCustomizer sqlServerHibernateCustomizer() {
        return properties ->
                properties.put("hibernate.jdbc.batch_versioned_data", "false");
    }
}
