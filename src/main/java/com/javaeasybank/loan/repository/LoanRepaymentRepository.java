package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    // 查某帳戶所有還款紀錄，按期數升序
    List<LoanRepayment> findByAccountIdOrderByPeriodIndexAsc(String accountId);

    // 查某帳戶特定狀態的還款紀錄
    List<LoanRepayment> findByAccountIdAndRepaymentStatus(String accountId, LoanRepaymentStatus status);

    // 查某帳戶的特定期數（還款時以 accountId + periodIndex 定位單筆）
    Optional<LoanRepayment> findByAccountIdAndPeriodIndex(String accountId, Integer periodIndex);

    // 查應繳日 <= 指定日期且狀態仍為 SCHEDULED 的紀錄（逾期掃描排程用）
    List<LoanRepayment> findByScheduledDateBeforeAndRepaymentStatus(LocalDate date, LoanRepaymentStatus status);
}