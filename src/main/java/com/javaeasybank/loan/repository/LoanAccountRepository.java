package com.javaeasybank.loan.repository;

/**
 * 程式說明：
 * - 貸款帳戶資料存取介面。
 * - 透過 Spring Data JPA 查詢貸款帳戶主檔與依狀態、客戶或申請編號篩選資料。
 */

import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 貸款帳戶資料存取介面
/**
 * 貸款帳戶資料存取介面。
 * 支援依客戶、申請編號、帳戶狀態與建立時間查詢正式成立的貸款帳戶。
 */
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
