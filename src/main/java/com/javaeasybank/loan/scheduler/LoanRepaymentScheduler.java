package com.javaeasybank.loan.scheduler;

import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.customer.service.CustomerService;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanRepaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 貸款還款排程批次：
 *   每日 01:00 掃描應繳日已過但仍為 SCHEDULED 的期數，標記為 OVERDUE，
 *   並同步將對應 LoanAccount.accountStatus 升級為 OVERDUE。
 *   同時掃描距應繳日 3 天內的期數，發送繳款到期提醒通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanRepaymentScheduler {

    private final LoanRepaymentRepository repaymentRepo;
    private final LoanAccountRepository   loanAccountRepo;
    private final EmailService            emailService;
    private final CustomerService         customerService;

    /**
     * 逾期掃描 + 到期提醒：每日 01:00 執行
     * cron = 秒 分 時 日 月 週
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void scanOverdueRepayments() {

        LocalDate today = LocalDate.now();
        log.info("[OverdueScan] 開始掃描 date={}", today);

        // ── 1. 逾期標記 ──────────────────────────────────────────
        // 應繳日 < 今天 且 仍為 SCHEDULED → 逾期
        List<LoanRepayment> overdues = repaymentRepo
                .findByScheduledDateBeforeAndRepaymentStatus(today, LoanRepaymentStatus.SCHEDULED);

        if (!overdues.isEmpty()) {
            // 批次標記 OVERDUE
            LocalDateTime now = LocalDateTime.now();
            overdues.forEach(rp -> {
                rp.setRepaymentStatus(LoanRepaymentStatus.OVERDUE);
                rp.setUpdateTime(now);
            });
            repaymentRepo.saveAll(overdues);

            // 將涉及的 LoanAccount 狀態升級為 OVERDUE（若目前仍是 ACTIVE）
            Map<String, List<LoanRepayment>> byAccount = overdues.stream()
                    .collect(Collectors.groupingBy(LoanRepayment::getAccountId));

            List<LoanAccount> accountsToUpdate = loanAccountRepo
                    .findAllById(byAccount.keySet())
                    .stream()
                    .filter(a -> a.getAccountStatus() == LoanAccountStatus.ACTIVE)
                    .collect(Collectors.toList());

            accountsToUpdate.forEach(a -> {
                a.setAccountStatus(LoanAccountStatus.OVERDUE);
                a.setUpdateTime(now);
            });
            loanAccountRepo.saveAll(accountsToUpdate);

            log.info("[OverdueScan] 完成：逾期期數={} 涉及帳戶={}",
                    overdues.size(), accountsToUpdate.size());

            // 逾期通知：每個逾期期數各寄一封
            Map<String, LoanAccount> accountMap = loanAccountRepo
                    .findAllById(byAccount.keySet())
                    .stream()
                    .collect(Collectors.toMap(LoanAccount::getAccountId, a -> a));

            overdues.forEach(rp -> {
                LoanAccount account = accountMap.get(rp.getAccountId());
                if (account == null) return;
                try {
                    String email = customerService.findEmailByCustomerId(account.getCustomerId());
                    if (email != null) {
                        emailService.sendLoanOverdueNotification(
                                email,
                                account.getAccountId(),
                                rp.getPeriodIndex(),
                                rp.getScheduledDate().toString(),
                                rp.getTotalAmount());
                    } else {
                        log.warn("[LoanOverdue] 客戶無 email，略過通知。customerId={}", account.getCustomerId());
                    }
                } catch (Exception e) {
                    log.error("[LoanOverdue] 發送逾期通知失敗，repaymentId={}", rp.getRepaymentId(), e);
                }
            });
        } else {
            log.info("[OverdueScan] 無逾期期數");
        }

        // ── 2. 到期提醒（距應繳日 1 ~ 3 天內）────────────────────
        LocalDate reminderStart = today.plusDays(1);
        LocalDate reminderEnd   = today.plusDays(3);

        List<LoanRepayment> upcoming = repaymentRepo
                .findByScheduledDateBetweenAndRepaymentStatus(
                        reminderStart, reminderEnd, LoanRepaymentStatus.SCHEDULED);

        if (upcoming.isEmpty()) {
            log.info("[ReminderScan] 無需提醒的期數");
            return;
        }

        log.info("[ReminderScan] 需提醒期數={}", upcoming.size());

        Map<String, LoanAccount> upcomingAccountMap = loanAccountRepo
                .findAllById(
                        upcoming.stream().map(LoanRepayment::getAccountId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(LoanAccount::getAccountId, a -> a));

        upcoming.forEach(rp -> {
            LoanAccount account = upcomingAccountMap.get(rp.getAccountId());
            if (account == null) return;
            try {
                String email = customerService.findEmailByCustomerId(account.getCustomerId());
                if (email != null) {
                    int daysLeft = (int) ChronoUnit.DAYS.between(today, rp.getScheduledDate());
                    emailService.sendLoanRepaymentReminderNotification(
                            email,
                            account.getAccountId(),
                            rp.getPeriodIndex(),
                            account.getConfirmedPeriod(),
                            rp.getScheduledDate().toString(),
                            daysLeft,
                            rp.getTotalAmount());
                } else {
                    log.warn("[LoanReminder] 客戶無 email，略過通知。customerId={}", account.getCustomerId());
                }
            } catch (Exception e) {
                log.error("[LoanReminder] 發送到期提醒失敗，repaymentId={}", rp.getRepaymentId(), e);
            }
        });

        log.info("[ReminderScan] 完成");
    }
}
