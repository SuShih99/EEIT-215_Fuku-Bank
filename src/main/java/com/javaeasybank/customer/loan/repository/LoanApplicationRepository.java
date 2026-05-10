package com.javaeasybank.customer.loan.repository;

import com.javaeasybank.customer.loan.entity.LoanApplication;
import com.javaeasybank.customer.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    // 依狀態篩選，按建立時間降序
    List<LoanApplication> findByApplicationStatusOrderByCreateTimeDesc(LoanApplicationStatus status);
}