package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.customer.service.CustomerService;
import com.javaeasybank.loan.dto.response.LoanRepaymentResponseDTO;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanRepaymentRepository;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
import com.javaeasybank.loan.utils.AmortizationCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// 還款期數業務邏輯 Service
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private NotificationService notificationService;

    // 依攤還表預建指定帳戶的所有還款期數，由 LoanAccountService 在撥款時呼叫
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

    // 依申請編號處理單期繳款（便利方法）
    public void processRepayment(String applicationId) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("Loan account not found applicationId=" + applicationId));
        processRepayments(account, 1);
    }

    // 依申請編號批次處理多期繳款
    public void processRepayments(String applicationId, int periodsToPay) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("Loan account not found applicationId=" + applicationId));
        processRepayments(account, periodsToPay);
    }

    // 依帳戶 ID 手動同步繳款（行員補同步用途）
    public void processRepaymentByAccountId(String accountId) {
        LoanAccount account = loanAccountRepo.findById(accountId)
                .orElseThrow(() -> new BusinessException("Loan account not found accountId=" + accountId));
        validateAccountingAlreadyDeducted(account);
        processRepayments(account, 1);
    }

    // 驗證帳務模組的負債餘額是否已等於「當前待繳期繳清後的剩餘本金」
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

    // 批次標記還款期數為已繳，並更新帳戶狀態與還款進度（核心邏輯）
    private void processRepayments(LoanAccount account, int periodsToPay) {
        if (periodsToPay <= 0) {
            throw new BusinessException("periodsToPay must be greater than 0");
        }

        List<LoanRepayment> pending = getPendingRepayments(account);
        List<LoanRepayment> paymentsToApply = pending.stream()
                .sorted(Comparator.comparing(LoanRepayment::getPeriodIndex))
                .limit(periodsToPay)
                .toList();

        if (paymentsToApply.size() < periodsToPay) {
            throw new BusinessException("Pending repayments are fewer than requested periods accountId="
                    + account.getAccountId());
        }

        LocalDate paidDate = LocalDate.now();
        paymentsToApply.forEach(repayment -> {
            repayment.setRepaymentStatus(LoanRepaymentStatus.PAID);
            repayment.setPaidDate(paidDate);
            repayment.setUpdateTime(LocalDateTime.now());
        });
        repaymentRepo.saveAll(paymentsToApply);

        LoanRepayment lastApplied = paymentsToApply.get(paymentsToApply.size() - 1);
        account.setPaidPeriods(account.getPaidPeriods() + paymentsToApply.size());
        account.setRemainingPrincipal(lastApplied.getRemainingAfter());

        // 下一期應繳日：從待繳清單中找比本次最後一期更大的最小期數
        account.setNextPaymentDate(
                pending.stream()
                        .filter(r -> r.getPeriodIndex() > lastApplied.getPeriodIndex())
                        .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                        .map(LoanRepayment::getScheduledDate)
                        .orElse(null));

        // 判斷本次標記後是否仍有其他逾期期數
        List<String> appliedIds = paymentsToApply.stream()
                .map(LoanRepayment::getRepaymentId)
                .toList();
        boolean hasOverdue = repaymentRepo
                .findByAccountIdAndRepaymentStatus(account.getAccountId(), LoanRepaymentStatus.OVERDUE)
                .stream()
                .anyMatch(r -> !appliedIds.contains(r.getRepaymentId()));

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

        // 還款成功通知（以本次最後一筆已繳期為準；失敗不影響主流程）
        try {
            String email = customerService.findEmailByCustomerId(account.getCustomerId());
            if (email != null) {
                LoanRepayment last = paymentsToApply.get(paymentsToApply.size() - 1);
                BigDecimal paidTotalAmount = paymentsToApply.stream()
                        .map(LoanRepayment::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal paidPrincipalAmount = paymentsToApply.stream()
                        .map(LoanRepayment::getPrincipalPortion)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal paidInterestAmount = paymentsToApply.stream()
                        .map(LoanRepayment::getInterestPortion)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String nextDateStr = account.getNextPaymentDate() != null
                        ? account.getNextPaymentDate().toString() : null;
                // 下期應繳金額：從還款排程取下一筆的 totalAmount
                java.math.BigDecimal nextAmt = pending.stream()
                        .filter(r -> r.getPeriodIndex() > last.getPeriodIndex())
                        .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                        .map(LoanRepayment::getTotalAmount)
                        .orElse(null);
                emailService.sendLoanRepaymentPaidNotification(
                        email,
                        account.getAccountId(),
                        last.getPeriodIndex(),
                        account.getConfirmedPeriod(),
                        paidTotalAmount,
                        paidPrincipalAmount,
                        paidInterestAmount,
                        nextDateStr,
                        nextAmt);
                notificationService.createNotification(
                        account.getCustomerId(),
                        NotificationType.LOAN,
                        "還款成功",
                        "本期貸款已完成繳款。",
                        "/user/loan-repayment?accountId=" + account.getAccountId());
            } else {
                log.warn("[RepaymentPaid] 客戶無 email，略過通知。customerId={}", account.getCustomerId());
            }
        } catch (Exception e) {
            log.error("[RepaymentPaid] 發送還款通知失敗，accountId={}", account.getAccountId(), e);
        }
    }

    // 取得指定帳戶所有待繳期數（SCHEDULED 或 OVERDUE）
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

    // 取得當前應繳的期數（期數最小的待繳記錄）
    private LoanRepayment getCurrentPendingRepayment(LoanAccount account) {
        return getCurrentPendingRepayment(account, getPendingRepayments(account));
    }

    // 從已取得的待繳清單中找出期數最小（即當前應繳）的期數
    private LoanRepayment getCurrentPendingRepayment(LoanAccount account, List<LoanRepayment> pending) {
        return pending.stream()
                .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                .orElseThrow(() -> new BusinessException("No pending repayment found accountId=" + account.getAccountId()));
    }

    // 取得指定帳戶的完整還款時間表，依期數升序排列
    @Transactional(readOnly = true)
    public List<LoanRepaymentResponseDTO> getByAccountId(String accountId) {
        return repaymentRepo.findByAccountIdOrderByPeriodIndexAsc(accountId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 全數還清後將關聯的貸款申請狀態更新為 CLOSED，並寄送結清通知 Email
    private void closeApplication(LoanAccount account) {
        log.info("[Repayment] paid off accountId={}", account.getAccountId());

        loanApplicationRepo.findById(account.getApplicationId()).ifPresent(loan -> {
            loan.setApplicationStatus(LoanApplicationStatus.CLOSED);
            loan.setUpdateTime(LocalDateTime.now());
            loanApplicationRepo.save(loan);
            log.info("[Repayment] closed applicationId={}", loan.getApplicationId());

            // 結清通知（失敗不影響主流程）
            try {
                String email = customerService.findEmailByCustomerId(account.getCustomerId());
                if (email != null) {
                    emailService.sendLoanPaidOffNotification(
                            email,
                            loan.getApplicationId(),
                            account.getAccountId(),
                            loan.getApplyType(),
                            account.getConfirmedPeriod());
                    notificationService.createNotification(
                            account.getCustomerId(),
                            NotificationType.LOAN,
                            "貸款已結清",
                            "您的貸款已全數結清。",
                            "/user/loan-accounts");
                } else {
                    log.warn("[LoanPaidOff] 客戶無 email，略過通知。customerId={}", account.getCustomerId());
                }
            } catch (Exception e) {
                log.error("[LoanPaidOff] 發送結清通知失敗，accountId={}", account.getAccountId(), e);
            }
        });
    }

    // 將 LoanRepayment Entity 轉換為回應 DTO
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

    // 產生還款期數識別碼：前綴 + UUID（去除橫線）
    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
