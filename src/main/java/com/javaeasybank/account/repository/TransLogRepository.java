package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.TransLog;
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

    /**
     * 檢查是否存在 note 中包含指定關鍵字的交易紀錄（用於沖正重複檢查）。
     *
     * @param keyword note 中的關鍵字。
     * @return 是否存在匹配的紀錄。
     */
    boolean existsByNoteContaining(String keyword);

    /**
     * 查詢與指定帳號相關的所有交易紀錄（包括作為影響帳號或對手方帳號的交易），並進行分頁。
     *
     * @param accountNumber 要查詢的帳號。
     * @param pageable      分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    @Query("SELECT t FROM TransLog t WHERE t.accountNumber = :acctNum OR t.counterpartAccount = :acctNum")
    Page<TransLog> findByAccountInvolved(@Param("acctNum") String accountNumber, Pageable pageable);

    /**
     * 根據客戶 ID 查詢該客戶名下所有帳戶的所有交易紀錄，並進行分頁。
     * 此查詢涉及跨表 JOIN。
     *
     * @param customerId 客戶 ID。
     * @param pageable   分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    @Query("SELECT t FROM TransLog t JOIN Account a ON t.accountNumber = a.accountNumber WHERE a.customerId = :customerId")
    Page<TransLog> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    /**
     * 根據客戶 ID 和日期範圍查詢該客戶名下所有帳戶的所有交易紀錄，並進行分頁。
     *
     * @param customerId 客戶 ID。
     * @param startDate  查詢的起始日期時間。
     * @param endDate    查詢的結束日期時間。
     * @param pageable   分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    @Query("SELECT t FROM TransLog t JOIN Account a ON t.accountNumber = a.accountNumber " +
           "WHERE a.customerId = :customerId AND t.createdAt >= :startDate AND t.createdAt <= :endDate")
    Page<TransLog> findByCustomerIdAndDateRange(
            @Param("customerId") String customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * [風險監控 B1] 短時間高頻交易偵測。
     * 統計指定時間後，某帳戶作為「發起方」(轉出)的特定交易類型次數。
     *
     * @param accountNumber   要統計的帳號。
     * @param entryType       記帳類型 (例如 DEBIT)。
     * @param transactionType 交易類型 (例如 TRANSFER)。
     * @param sinceTime       起始時間點。
     * @return 匹配的交易次數。
     */
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

    /**
     * [風險監控 B2] 24 小時累積金額偵測。
     * 統計指定時間後，某帳戶作為「轉出方」(轉帳或提款)的累積金額。
     *
     * @param accountNumber   要統計的帳號。
     * @param entryType       記帳類型 (例如 DEBIT)。
     * @param transactionTypes 交易類型列表 (例如 TRANSFER, WITHDRAW)。
     * @param sinceTime       起始時間點。
     * @return 匹配的累積金額，如果沒有交易則返回 0。
     */
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

    /**
     * 查詢所有交易紀錄並按建立時間倒序排序，並進行分頁。
     *
     * @param pageable 分頁資訊。
     * @return 包含交易日誌實體的分頁列表。
     */
    Page<TransLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}