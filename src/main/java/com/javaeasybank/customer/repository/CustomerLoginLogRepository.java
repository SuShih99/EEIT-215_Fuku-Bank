package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerLoginLogRepository extends JpaRepository<CustomerLoginLog, Long> {
    List<CustomerLoginLog> findTop30ByCustomerIdOrderByLoginTimeDesc(String customerId);
}
