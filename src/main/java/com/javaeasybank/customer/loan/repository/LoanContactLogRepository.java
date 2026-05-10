package com.javaeasybank.customer.loan.repository;

import com.javaeasybank.customer.loan.entity.LoanContactLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanContactLogRepository extends JpaRepository<LoanContactLog, String> {

    // 查某申請的所有聯繫紀錄，按時間降序
    List<LoanContactLog> findByApplicationIdOrderByContactTimeDesc(String applicationId);
}