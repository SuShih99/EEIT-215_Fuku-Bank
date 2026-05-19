package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 還款期數資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanRepayment} 的 CRUD 操作。
 * 主要由 {@code LoanRepaymentService} 與 {@code LoanRepaymentScheduler} 使用，
 * 支援還款時間表查詢、逾期掃描與到期提醒等業務場景。</p>
 */
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    /**
     * 查詢指定帳戶的所有還款期數，依期數升序排列（第一期在前）。
     * 用於前端顯示完整還款時間表。
     *
     * @param accountId 貸款帳戶識別碼
     * @return 該帳戶的所有還款期數清單
     */
    List<LoanRepayment> findByAccountIdOrderByPeriodIndexAsc(String accountId);

    /**
     * 查詢指定帳戶中特定狀態的還款期數。
     * 例如查詢所有 {@code OVERDUE} 的期數以計算逾期金額。
     *
     * @param accountId 貸款帳戶識別碼
     * @param status    還款狀態
     * @return 符合條件的還款期數清單
     */
    List<LoanRepayment> findByAccountIdAndRepaymentStatus(String accountId, LoanRepaymentStatus status);

    /**
     * 查詢指定帳戶中屬於多種狀態之一的還款期數。
     * 例如同時查詢 {@code SCHEDULED} 與 {@code OVERDUE}，用於計算未還款總額。
     *
     * @param accountId 貸款帳戶識別碼
     * @param statuses  狀態清單
     * @return 符合任一狀態的還款期數清單
     */
    List<LoanRepayment> findByAccountIdAndRepaymentStatusIn(String accountId, List<LoanRepaymentStatus> statuses);

    /**
     * 依帳戶 ID 與期數查詢單筆還款紀錄。
     * 用於客戶繳款時定位當期的應繳紀錄。
     *
     * @param accountId   貸款帳戶識別碼
     * @param periodIndex 期數（1-based）
     * @return 對應的還款紀錄，不存在時為 {@code Optional.empty()}
     */
    Optional<LoanRepayment> findByAccountIdAndPeriodIndex(String accountId, Integer periodIndex);

    /**
     * 查詢所有應繳日早於指定日期且狀態為指定值的還款期數。
     * 供 {@code LoanRepaymentScheduler} 每日掃描逾期未繳的期數（應繳日 &lt; 今日 且 {@code SCHEDULED}）。
     *
     * @param date   截止日期（通常傳入今天，查詢今天之前尚未繳款的期數）
     * @param status 還款狀態（通常為 {@code SCHEDULED}）
     * @return 符合條件的逾期還款期數清單
     */
    List<LoanRepayment> findByScheduledDateBeforeAndRepaymentStatus(LocalDate date, LoanRepaymentStatus status);

    /**
     * 查詢應繳日介於 {@code startDate}（含）與 {@code endDate}（含）之間，
     * 且狀態為指定值的還款期數。
     * 用於到期提醒，例如查詢未來 7 天即將到期的 {@code SCHEDULED} 期數。
     *
     * @param startDate 應繳日範圍起始日期
     * @param endDate   應繳日範圍結束日期
     * @param status    還款狀態（通常為 {@code SCHEDULED}）
     * @return 符合條件的還款期數清單
     */
    List<LoanRepayment> findByScheduledDateBetweenAndRepaymentStatus(
            LocalDate startDate, LocalDate endDate, LoanRepaymentStatus status);
}
