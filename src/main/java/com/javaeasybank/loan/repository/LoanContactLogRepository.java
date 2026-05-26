package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanContactLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 聯繫紀錄資料存取介面
public interface LoanContactLogRepository extends JpaRepository<LoanContactLog, String> {

    // 查詢指定申請的所有聯繫紀錄，按聯繫時間降序排列（最新的在前）
    List<LoanContactLog> findByApplicationIdOrderByContactTimeDesc(String applicationId);
}
