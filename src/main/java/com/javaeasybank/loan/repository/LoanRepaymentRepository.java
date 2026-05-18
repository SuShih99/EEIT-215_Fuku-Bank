package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    List<LoanRepayment> findByAccountIdOrderByPeriodIndexAsc(String accountId);

    List<LoanRepayment> findByAccountIdAndRepaymentStatus(String accountId, LoanRepaymentStatus status);

    List<LoanRepayment> findByAccountIdAndRepaymentStatusIn(String accountId, List<LoanRepaymentStatus> statuses);

    Optional<LoanRepayment> findByAccountIdAndPeriodIndex(String accountId, Integer periodIndex);

    List<LoanRepayment> findByScheduledDateBeforeAndRepaymentStatus(LocalDate date, LoanRepaymentStatus status);
}
