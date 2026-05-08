package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    // 依 applicationId 查單筆
    Optional<LoanAccount> findByApplicationId(String applicationId);

    // 依 customerId 查該客戶所有貸款帳戶，按建立時間降序
    List<LoanAccount> findByCustomerIdOrderByCreateTimeDesc(String customerId);
}