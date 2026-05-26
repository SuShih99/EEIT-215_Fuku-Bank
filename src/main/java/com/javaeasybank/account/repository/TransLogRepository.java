package com.javaeasybank.account.repository;

import com.javaeasybank.account.dto.request.AccountStats;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易日誌資料庫操作接口。
 * 繼承 JpaRepository 提供基本的 CRUD 操作，並繼承 JpaSpecificationExecutor 提供複雜查詢能力。
 */
@Repository
public interface TransLogRepository extends JpaRepository<TransLog, String>, JpaSpecificationExecutor<TransLog> {

    /**
     * 根據參考 ID 查詢單筆或多筆交易紀錄。
     * 例如，一筆轉帳交易會產生兩筆具有相同 reference_id 的紀錄（轉出和轉入）。
     *
     * @param referenceId 業務交易參考 ID。
     * @return 匹配的交易日誌列表。
     */
    List<TransLog> findByReferenceId(String referenceId);

    @Query("""
            SELECT t
            FROM TransLog t
            LEFT JOIN Account a ON t.accountNumber = a.accountNumber
            WHERE t.referenceId = :referenceId
              AND (a.accountType IS NULL OR a.accountType <> :excludedType)
            """)
    List<TransLog> findByReferenceIdExcludingAccountType(
            @Param("referenceId") String referenceId,
            @Param("excludedType") AccountType excludedType);

    /**
     * 檢查是否存在 note 中包含指定關鍵字的交易紀錄（用於沖正重複檢查）。
     *
     * @param keyword note 中的關鍵字。
     * @return 是否存在匹配的紀錄。
     */
    boolean existsByNoteContaining(String keyword);

    /**
     * 檢查是否存在指定交易類型且 note 包含關鍵字的交易紀錄。
     *
     * @param transactionType 交易類型。
     * @param keyword         note 中的關鍵字。
     * @return 是否存在匹配的紀錄。
     */
    boolean existsByTransactionTypeAndNoteContaining(TransactionType transactionType, String keyword);

    /**
     * 查詢與指定帳號相關的所有交易紀錄（包括作為影響帳號或對手方帳號的交易），並進行分頁。
     *
     * @param accountNumber 要查詢的帳號。
     * @param pageable      分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    @Query("SELECT t FROM TransLog t WHERE t.accountNumber = :acctNum OR t.counterpartAccount = :acctNum")
    Page<TransLog> findByAccountInvolved(@Param("acctNum") String accountNumber, Pageable pageable);

    @Query("""
            SELECT t
            FROM TransLog t
            LEFT JOIN Account a ON t.accountNumber = a.accountNumber
            WHERE (t.accountNumber = :acctNum OR t.counterpartAccount = :acctNum)
              AND (a.accountType IS NULL OR a.accountType <> :excludedType)
            """)
    Page<TransLog> findByAccountInvolvedExcludingAccountType(
            @Param("acctNum") String accountNumber,
            @Param("excludedType") AccountType excludedType,
            Pageable pageable);


    @Query("SELECT t FROM TransLog t JOIN Account a ON t.accountNumber = a.accountNumber WHERE a.customerId = :customerId")
    Page<TransLog> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    @Query("""
            SELECT t
            FROM TransLog t
            JOIN Account a ON t.accountNumber = a.accountNumber
            WHERE a.customerId = :customerId
              AND a.accountType <> :excludedType
            """)
    Page<TransLog> findByCustomerIdExcludingAccountType(
            @Param("customerId") String customerId,
            @Param("excludedType") AccountType excludedType,
            Pageable pageable);


    @Query("SELECT t FROM TransLog t JOIN Account a ON t.accountNumber = a.accountNumber " +
            "WHERE a.customerId = :customerId AND t.createdAt >= :startDate AND t.createdAt <= :endDate")
    Page<TransLog> findByCustomerIdAndDateRange(
            @Param("customerId") String customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM TransLog t
            JOIN Account a ON t.accountNumber = a.accountNumber
            WHERE a.customerId = :customerId
              AND a.accountType <> :excludedType
              AND t.createdAt >= :startDate
              AND t.createdAt <= :endDate
            """)
    Page<TransLog> findByCustomerIdAndDateRangeExcludingAccountType(
            @Param("customerId") String customerId,
            @Param("excludedType") AccountType excludedType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);


    @Query("SELECT COUNT(t) FROM TransLog t " +
            "WHERE t.accountNumber = :accountNumber " +
            "AND t.entryType = :entryType " +
            "AND t.transactionType = :transactionType " +
            "AND t.createdAt > :sinceTime")
    int countRecentTransactions(
            @Param("accountNumber") String accountNumber,
            @Param("entryType") EntryType entryType,
            @Param("transactionType") TransactionType transactionType,
            @Param("sinceTime") LocalDateTime sinceTime);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransLog t " +
            "WHERE t.accountNumber = :accountNumber " +
            "AND t.entryType = :entryType " +
            "AND t.transactionType IN :transactionTypes " +
            "AND t.createdAt > :sinceTime")
    BigDecimal sumRecentOutflow(
            @Param("accountNumber") String accountNumber,
            @Param("entryType") EntryType entryType,
            @Param("transactionTypes") List<TransactionType> transactionTypes,
            @Param("sinceTime") LocalDateTime sinceTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransLog t " +
            "WHERE t.accountNumber = :accountNumber " +
            "AND t.transactionType = :transactionType " +
            "AND t.entryType = :entryType " +
            "AND (:startAt IS NULL OR t.createdAt >= :startAt) " +
            "AND (:endAt IS NULL OR t.createdAt <= :endAt)")
    BigDecimal sumAmountByAccountAndType(
            @Param("accountNumber") String accountNumber,
            @Param("transactionType") TransactionType transactionType,
            @Param("entryType") EntryType entryType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    /**
     * 查詢所有交易紀錄並按建立時間倒序排序，並進行分頁。
     *
     * @param pageable 分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    Page<TransLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT t
            FROM TransLog t
            LEFT JOIN Account a ON t.accountNumber = a.accountNumber
            WHERE a.accountType IS NULL OR a.accountType <> :excludedType
            ORDER BY t.createdAt DESC
            """)
    Page<TransLog> findAllExcludingAccountTypeOrderByCreatedAtDesc(
            @Param("excludedType") AccountType excludedType,
            Pageable pageable);

    /**
     * 一次性取得指定時間內的交易次數與總額
     */
    @Query("SELECT new com.javaeasybank.account.dto.request.AccountStats(COUNT(t), SUM(t.amount)) " +
            "FROM TransLog t " +
            "WHERE t.accountNumber = :accountNumber " +
            "AND t.entryType = :entryType " +
            "AND t.transactionType = :transactionType " +
            "AND t.createdAt > :sinceTime")
    AccountStats getRecentStats(
            @Param("accountNumber") String accountNumber,
            @Param("entryType") EntryType entryType,
            @Param("transactionType") TransactionType transactionType,
            @Param("sinceTime") LocalDateTime sinceTime);
    // 統計用 (Dashboard)
    @Query("SELECT t.entryType, COUNT(t) FROM TransLog t GROUP BY t.entryType")
    List<Object[]> countByEntryTypeGroup();

    @Query("SELECT t.transactionType, COUNT(t) FROM TransLog t GROUP BY t.transactionType")
    List<Object[]> countByTransactionTypeGroup();

    @Query("SELECT t.currency, COUNT(t) FROM TransLog t GROUP BY t.currency")
    List<Object[]> countByCurrencyGroup();

    @Query("SELECT SUM(t.amount) FROM TransLog t WHERE t.entryType = 'CREDIT'")
    BigDecimal sumTotalCredit();

    @Query("SELECT SUM(t.amount) FROM TransLog t WHERE t.entryType = 'DEBIT'")
    BigDecimal sumTotalDebit();

    @Query("SELECT MAX(t.amount) FROM TransLog t")
    BigDecimal maxTransactionAmount();
}
