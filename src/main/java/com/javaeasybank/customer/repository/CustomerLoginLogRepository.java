package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerLoginLogRepository extends JpaRepository<CustomerLoginLog, Long> {
    List<CustomerLoginLog> findTop30ByCustomerIdOrderByLoginTimeDesc(String customerId);

    // 查最近 N 分鐘內失敗次數
    @Query("SELECT COUNT(l) FROM CustomerLoginLog l " +
            "WHERE l.username = :username " +
            "AND l.result = '失敗' " +
            "AND l.loginTime >= :since")
    int countRecentFailures(
            @Param("username") String username,
            @Param("since") LocalDateTime since);
}