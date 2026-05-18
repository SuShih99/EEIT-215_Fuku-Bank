package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.loan.dto.response.LoanRepaymentResponseDTO;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanRepaymentRepository;
import com.javaeasybank.loan.utils.AmortizationCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LoanRepaymentService {

    @Autowired
    private LoanRepaymentRepository repaymentRepo;

    @Autowired
    private LoanAccountRepository loanAccountRepo;

    @Autowired
    private LoanApplicationRepository loanApplicationRepo;

    @Autowired
    private AccountRepository accountRepo;

    public void createSchedule(LoanAccount account) {
        List<AmortizationCalculator.RepaymentRow> rows = AmortizationCalculator.buildSchedule(
                account.getRemainingPrincipal(),
                account.getRate(),
                account.getConfirmedPeriod(),
                account.getNextPaymentDate());

        List<LoanRepayment> repayments = rows.stream().map(row -> {
            LoanRepayment rp = new LoanRepayment();
            rp.setRepaymentId(generateId("RP"));
            rp.setAccountId(account.getAccountId());
            rp.setPeriodIndex(row.periodIndex());
            rp.setScheduledDate(row.scheduledDate());
            rp.setTotalAmount(row.totalAmount());
            rp.setPrincipalPortion(row.principalPortion());
            rp.setInterestPortion(row.interestPortion());
            rp.setRemainingAfter(row.remainingAfter());
            rp.setRepaymentStatus(LoanRepaymentStatus.SCHEDULED);
            rp.setCreateTime(LocalDateTime.now());
            return rp;
        }).collect(Collectors.toList());

        repaymentRepo.saveAll(repayments);
        log.info("[RepaymentSchedule] created accountId={} periods={}",
                account.getAccountId(), repayments.size());
    }

    public void processRepayment(String applicationId) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("Loan account not found applicationId=" + applicationId));
        processRepayment(account);
    }

    public void processRepaymentByAccountId(String accountId) {
        LoanAccount account = loanAccountRepo.findById(accountId)
                .orElseThrow(() -> new BusinessException("Loan account not found accountId=" + accountId));
        validateAccountingAlreadyDeducted(account);
        processRepayment(account);
    }

    private void validateAccountingAlreadyDeducted(LoanAccount account) {
        Account accountingLoanAccount = accountRepo.findById(account.getAccountNumber())
                .orElseThrow(() -> new BusinessException("Accounting loan account not found accountNumber="
                        + account.getAccountNumber()));
        LoanRepayment current = getCurrentPendingRepayment(account);
        if (accountingLoanAccount.getLiability() == null
                || accountingLoanAccount.getLiability().compareTo(current.getRemainingAfter()) != 0) {
            throw new BusinessException("帳務負債與待補同步期別不一致，無法補同步");
        }
    }

    private void processRepayment(LoanAccount account) {
        List<LoanRepayment> pending = getPendingRepayments(account);
        LoanRepayment current = getCurrentPendingRepayment(account, pending);

        current.setRepaymentStatus(LoanRepaymentStatus.PAID);
        current.setPaidDate(LocalDate.now());
        current.setUpdateTime(LocalDateTime.now());
        repaymentRepo.save(current);

        account.setPaidPeriods(account.getPaidPeriods() + 1);
        account.setRemainingPrincipal(current.getRemainingAfter());

        account.setNextPaymentDate(
                pending.stream()
                        .filter(r -> r.getPeriodIndex() > current.getPeriodIndex())
                        .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                        .map(LoanRepayment::getScheduledDate)
                        .orElse(null));

        boolean hasOverdue = repaymentRepo
                .findByAccountIdAndRepaymentStatus(account.getAccountId(), LoanRepaymentStatus.OVERDUE)
                .stream()
                .anyMatch(r -> !r.getRepaymentId().equals(current.getRepaymentId()));

        if (account.getPaidPeriods().equals(account.getConfirmedPeriod())) {
            account.setAccountStatus(LoanAccountStatus.PAID_OFF);
            closeApplication(account);
        } else if (hasOverdue) {
            account.setAccountStatus(LoanAccountStatus.OVERDUE);
        } else {
            account.setAccountStatus(LoanAccountStatus.ACTIVE);
        }

        account.setUpdateTime(LocalDateTime.now());
        loanAccountRepo.save(account);

        log.info("[Repayment] updated accountId={} paidPeriods={}/{} status={}",
                account.getAccountId(),
                account.getPaidPeriods(),
                account.getConfirmedPeriod(),
                account.getAccountStatus());
    }

    private List<LoanRepayment> getPendingRepayments(LoanAccount account) {
        List<LoanRepayment> pending = repaymentRepo.findByAccountIdAndRepaymentStatusIn(
                account.getAccountId(),
                List.of(LoanRepaymentStatus.SCHEDULED, LoanRepaymentStatus.OVERDUE));
        if (pending.isEmpty()) {
            log.warn("[Repayment] no pending repayment found accountId={}", account.getAccountId());
            throw new BusinessException("No pending repayment found accountId=" + account.getAccountId());
        }
        return pending;
    }

    private LoanRepayment getCurrentPendingRepayment(LoanAccount account) {
        return getCurrentPendingRepayment(account, getPendingRepayments(account));
    }

    private LoanRepayment getCurrentPendingRepayment(LoanAccount account, List<LoanRepayment> pending) {
        return pending.stream()
                .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                .orElseThrow(() -> new BusinessException("No pending repayment found accountId=" + account.getAccountId()));
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponseDTO> getByAccountId(String accountId) {
        return repaymentRepo.findByAccountIdOrderByPeriodIndexAsc(accountId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private void closeApplication(LoanAccount account) {
        log.info("[Repayment] paid off accountId={}", account.getAccountId());

        loanApplicationRepo.findById(account.getApplicationId()).ifPresent(loan -> {
            loan.setApplicationStatus(LoanApplicationStatus.CLOSED);
            loan.setUpdateTime(LocalDateTime.now());
            loanApplicationRepo.save(loan);
            log.info("[Repayment] closed applicationId={}", loan.getApplicationId());
        });
    }

    private LoanRepaymentResponseDTO toResponseDTO(LoanRepayment rp) {
        LoanRepaymentResponseDTO dto = new LoanRepaymentResponseDTO();
        dto.setRepaymentId(rp.getRepaymentId());
        dto.setAccountId(rp.getAccountId());
        dto.setPeriodIndex(rp.getPeriodIndex());
        dto.setScheduledDate(rp.getScheduledDate());
        dto.setPaidDate(rp.getPaidDate());
        dto.setTotalAmount(rp.getTotalAmount());
        dto.setPrincipalPortion(rp.getPrincipalPortion());
        dto.setInterestPortion(rp.getInterestPortion());
        dto.setRemainingAfter(rp.getRemainingAfter());
        dto.setRepaymentStatus(rp.getRepaymentStatus());
        return dto;
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
