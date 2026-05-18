package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 帳戶資料庫操作接口。
 * 繼承 JpaRepository，提供基本的 CRUD 操作以及自定義查詢方法。
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * 根據客戶 ID 查詢該客戶名下所有帳戶並進行分頁。
     *
     * @param customerId 客戶 ID。
     * @param pageable   分頁資訊。
     * @return 包含帳戶實體的分頁列表。
     */
    Page<Account> findByCustomerId(String customerId, Pageable pageable);

    Page<Account> findByCustomerIdAndAccountTypeNotIn(String customerId, Collection<AccountType> accountTypes, Pageable pageable);

    Page<Account> findByAccountTypeNotOrderByCreatedAtDesc(AccountType accountType, Pageable pageable);

    List<Account> findAllByAccountNumberInAndAccountType(Collection<String> accountNumbers, AccountType accountType);

    boolean existsByAccountNumberAndAccountType(String accountNumber, AccountType accountType);

    /**
     * 根據帳戶狀態篩選帳戶並進行分頁。
     *
     * @param status   要篩選的帳戶狀態。
     * @param pageable 分頁資訊。
     * @return 包含帳戶實體的分頁列表。
     */
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    Page<Account> findByStatusAndAccountTypeNot(AccountStatus status, AccountType accountType, Pageable pageable);

    /**
     * 根據帳戶類型和貨幣篩選帳戶並進行分頁。
     *
     * @param accountType 要篩選的帳戶類型。
     * @param currency    要篩選的貨幣。
     * @param pageable    分頁資訊。
     * @return 包含帳戶實體的分頁列表。
     */
    Page<Account> findByAccountTypeAndCurrency(AccountType accountType, Currency currency, Pageable pageable);

    @Query("""
            SELECT a
            FROM Account a
            WHERE a.accountType NOT IN :excludedTypes
              AND (:customerId IS NULL OR LOWER(a.customerId) LIKE LOWER(CONCAT('%', :customerId, '%')))
              AND (:customerName IS NULL OR EXISTS (
                    SELECT 1
                    FROM CustomerProfile cp
                    WHERE cp.customerId = a.customerId
                      AND LOWER(cp.name) LIKE LOWER(CONCAT('%', :customerName, '%'))
              ))
              AND (:accountNumber IS NULL OR a.accountNumber LIKE CONCAT('%', :accountNumber, '%'))
              AND (:status IS NULL OR a.status = :status)
              AND (:accountType IS NULL OR a.accountType = :accountType)
              AND (:currency IS NULL OR a.currency = :currency)
            ORDER BY a.createdAt DESC
            """)
    Page<Account> searchAdminAccounts(
            @Param("customerId") String customerId,
            @Param("customerName") String customerName,
            @Param("accountNumber") String accountNumber,
            @Param("status") AccountStatus status,
            @Param("accountType") AccountType accountType,
            @Param("currency") Currency currency,
            @Param("excludedTypes") Collection<AccountType> excludedTypes,
            Pageable pageable);

    boolean existsByCustomerIdAndAccountType(String customerId, AccountType accountType);

    Optional<Account> findFirstByCustomerIdAndAccountType(String customerId, AccountType accountType);

    List<Account> findAllByCustomerIdAndAccountType(String customerId, AccountType accountType);

    List<Account> findAllByCustomerIdAndAccountTypeAndCurrencyAndStatus(
            String customerId,
            AccountType accountType,
            Currency currency,
            AccountStatus status);

    /**
     * 檢查指定客戶是否已擁有特定帳戶類型和貨幣的帳戶。
     *
     * @param customerId  客戶 ID。
     * @param accountType 帳戶類型。
     * @param currency    貨幣。
     * @return 如果存在則返回 true，否則返回 false。
     */
    boolean existsByCustomerIdAndAccountTypeAndCurrency(String customerId, AccountType accountType, Currency currency);

    /**
     * 檢查指定客戶是否擁有特定帳戶類型、貨幣和狀態的帳戶。
     *
     * @param customerId  客戶 ID。
     * @param accountType 帳戶類型。
     * @param currency    貨幣。
     * @param status      帳戶狀態。
     * @return 如果存在則返回 true，否則返回 false。
     */
    boolean existsByCustomerIdAndAccountTypeAndCurrencyAndStatus(String customerId, AccountType accountType, Currency currency, AccountStatus status);

    /**
     * 查詢所有帳戶並按建立時間倒序排序（最新建立的排在前面），並進行分頁。
     *
     * @param pageable 分頁資訊。
     * @return 包含帳戶實體的分頁列表。
     */
    Page<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根據客戶 ID 查詢該客戶名下所有帳戶（不分頁）。
     *
     * @param customerId 客戶 ID。
     * @return 帳戶列表。
     */
    List<Account> findAllByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber IN :accountNumbers ORDER BY a.accountNumber")
    List<Account> findAllByAccountNumberInForUpdate(@Param("accountNumbers") Collection<String> accountNumbers);
}
