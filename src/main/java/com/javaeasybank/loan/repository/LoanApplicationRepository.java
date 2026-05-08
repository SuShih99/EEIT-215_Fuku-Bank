package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    // 依狀態篩選，按建立時間降序
    List<LoanApplication> findByApplicationStatusOrderByCreateTimeDesc(LoanApplicationStatus status);

    List<LoanApplication> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    // 查 updateTime 不為 null 的申請，依 updateTime 降序（置頂用）
    List<LoanApplication> findByUpdateTimeIsNotNullOrderByUpdateTimeDesc();
}