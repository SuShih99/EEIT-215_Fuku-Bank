package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    Optional<LoanAccount> findByApplicationId(String applicationId);

    Optional<LoanAccount> findByAccountNumber(String accountNumber);

    List<LoanAccount> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    List<LoanAccount> findByAccountStatusOrderByCreateTimeDesc(LoanAccountStatus accountStatus);
}
