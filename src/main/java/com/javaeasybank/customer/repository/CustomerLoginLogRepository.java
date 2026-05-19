package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerLoginLogRepository extends JpaRepository<CustomerLoginLog, Long> {
    List<CustomerLoginLog> findTop30ByCustomerIdOrderByLoginTimeDesc(String customerId);

    @Query("SELECT COUNT(l) FROM CustomerLoginLog l " +
            "WHERE l.customerId = :customerId " +
            "AND l.result = '失敗' " +
            "AND l.loginTime >= :since " +
            "AND (:unlockedAt IS NULL OR l.loginTime > :unlockedAt)")
    int countRecentFailures(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since,
            @Param("unlockedAt") LocalDateTime unlockedAt);
}