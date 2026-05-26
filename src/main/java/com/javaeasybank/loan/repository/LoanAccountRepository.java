package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 貸款帳戶資料存取介面
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    // 依關聯的貸款申請 ID 查詢帳戶
    Optional<LoanAccount> findByApplicationId(String applicationId);

    // 依貸款帳號查詢帳戶（帳號為對外唯一識別號）
    Optional<LoanAccount> findByAccountNumber(String accountNumber);

    // 查詢指定客戶的所有貸款帳戶，依建立時間降序排列（最新的在前）
    List<LoanAccount> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    // 依帳戶狀態查詢所有符合的貸款帳戶，依建立時間降序排列
    List<LoanAccount> findByAccountStatusOrderByCreateTimeDesc(LoanAccountStatus accountStatus);
}
