package com.javaeasybank.loan.repository;

/**
 * 程式說明：
 * - 貸款還款資料存取介面。
 * - 支援依貸款帳戶、到期日與繳款狀態查詢還款明細，供排程與前端使用。
 */

import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 還款期數資料存取介面
/**
 * 還款期數資料存取介面。
 * 提供依帳戶、期數狀態、到期日查詢，用於還款頁、排程逾期掃描與到期提醒。
 */
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    // 查詢指定帳戶的所有還款期數，依期數升序排列（第一期在前）
    List<LoanRepayment> findByAccountIdOrderByPeriodIndexAsc(String accountId);

    // 查詢指定帳戶中特定狀態的還款期數
    List<LoanRepayment> findByAccountIdAndRepaymentStatus(String accountId, LoanRepaymentStatus status);

    // 查詢指定帳戶中屬於多種狀態之一的還款期數
    List<LoanRepayment> findByAccountIdAndRepaymentStatusIn(String accountId, List<LoanRepaymentStatus> statuses);

    // 依帳戶 ID 與期數查詢單筆還款紀錄
    Optional<LoanRepayment> findByAccountIdAndPeriodIndex(String accountId, Integer periodIndex);

    // 查詢所有應繳日早於指定日期且狀態為指定值的還款期數
    List<LoanRepayment> findByScheduledDateBeforeAndRepaymentStatus(LocalDate date, LoanRepaymentStatus status);

    // 查詢應繳日介於 startDate（含）與 endDate（含）之間，
    List<LoanRepayment> findByScheduledDateBetweenAndRepaymentStatus(
            LocalDate startDate, LocalDate endDate, LoanRepaymentStatus status);
}
