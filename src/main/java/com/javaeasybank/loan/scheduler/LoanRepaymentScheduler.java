package com.javaeasybank.loan.scheduler;

/**
 * 程式說明：
 * - 貸款還款排程服務。
 * - 定期檢查到期或逾期還款資料，更新狀態並通知相關會員。
 */

import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.customer.service.CustomerService;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanRepayment;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.enums.LoanRepaymentStatus;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanRepaymentRepository;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
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

// 貸款還款排程批次元件
@Slf4j
/**
 * 貸款還款排程。
 * 每日掃描逾期期數並寄出逾期通知，同時提醒即將到期的待繳期數。
 */
@Component
@RequiredArgsConstructor
public class LoanRepaymentScheduler {

    private final LoanRepaymentRepository repaymentRepo;
    private final LoanAccountRepository   loanAccountRepo;
    private final EmailService            emailService;
    private final CustomerService         customerService;
    private final NotificationService     notificationService;

    // 逾期掃描與到期提醒的主排程方法，每日 01:00 自動執行
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void scanOverdueRepayments() {

        LocalDate today = LocalDate.now();
        log.info("[OverdueScan] 開始掃描 date={}", today);
        // 應繳日 < 今天 且 仍為 SCHEDULED → 標記為 OVERDUE
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

            // 逾期通知：每個逾期期數各寄一封通知給客戶
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
                        notificationService.createNotification(
                                account.getCustomerId(),
                                NotificationType.LOAN,
                                "貸款逾期通知",
                                "您有一期貸款已逾期，請儘速處理。",
                                "/user/loan-repayment?accountId=" + account.getAccountId());
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

        // 對每筆即將到期的期數寄送提醒 Email
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
                    notificationService.createNotification(
                            account.getCustomerId(),
                            NotificationType.LOAN,
                            "貸款到期提醒",
                            "您有一期貸款即將到期。",
                            "/user/loan-repayment?accountId=" + account.getAccountId());
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
