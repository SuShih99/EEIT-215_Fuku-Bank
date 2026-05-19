package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 貸款帳戶資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanAccount} 的 CRUD 操作，
 * 並依業務需求定義常用的衍生查詢方法。</p>
 */
public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    /**
     * 依關聯的貸款申請 ID 查詢帳戶。
     * 一筆申請撥款後僅會建立一個帳戶，故回傳 {@code Optional}。
     *
     * @param applicationId 貸款申請識別碼
     * @return 對應的貸款帳戶，若尚未撥款則為 {@code Optional.empty()}
     */
    Optional<LoanAccount> findByApplicationId(String applicationId);

    /**
     * 依貸款帳號查詢帳戶（帳號為對外唯一識別號）。
     *
     * @param accountNumber 貸款帳號
     * @return 對應的貸款帳戶，不存在時為 {@code Optional.empty()}
     */
    Optional<LoanAccount> findByAccountNumber(String accountNumber);

    /**
     * 查詢指定客戶的所有貸款帳戶，依建立時間降序排列（最新的在前）。
     *
     * @param customerId 客戶內部識別碼
     * @return 該客戶的貸款帳戶清單，無資料時回傳空清單
     */
    List<LoanAccount> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    /**
     * 依帳戶狀態查詢所有符合的貸款帳戶，依建立時間降序排列。
     * 常用於行政後台篩選逾期帳戶（{@code OVERDUE}）進行催繳作業。
     *
     * @param accountStatus 帳戶狀態，參見 {@code LoanAccountStatus}
     * @return 符合狀態的貸款帳戶清單，無資料時回傳空清單
     */
    List<LoanAccount> findByAccountStatusOrderByCreateTimeDesc(LoanAccountStatus accountStatus);
}
