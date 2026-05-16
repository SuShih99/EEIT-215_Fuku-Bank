package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.response.LoanRepaymentResponseDTO;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanApplication;
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

/*
 * 負責 LoanRepayment 的建立、還款進度回寫與查詢。
 */
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

    /**
     * 撥款後一次性預排 N 期還款明細，status 全部設為 SCHEDULED。
     * 由 LoanAccountService.createOnDisbursement() 在同一個事務內呼叫。
     *
     * @param account 剛建立完成的 LoanAccount（需含 rate、confirmedPeriod、nextPaymentDate）
     */
    public void createSchedule(LoanAccount account) {

        List<AmortizationCalculator.RepaymentRow> rows = AmortizationCalculator.buildSchedule(
                account.getRemainingPrincipal(),    // 撥款本金
                account.getRate(),                  // 年利率
                account.getConfirmedPeriod(),        // 還款總期數
                account.getNextPaymentDate()         // 第 1 期應繳日（= startDate + 1 個月）
        );

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
        log.info("[RepaymentSchedule] 預排完成 accountId={} periods={}",
                account.getAccountId(), repayments.size());
    }

    // ===還款進度回寫===

    /**
     * Account 模組還款成功後通知呼叫。
     * 定位當期最小待繳期數 → 標記 PAID → 更新 LoanAccount 進度。
     * 若全數繳清則將帳戶標為 PAID_OFF。
     *
     * @param applicationId Loan 模組申請編號（透過 LoanDisbursementRequest.applicationId 傳入）
     */
    public void processRepayment(String applicationId) {

        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("找不到貸款帳戶：" + applicationId));

        // 取得所有 SCHEDULED 期數，找最小 periodIndex（即當期應繳）
        List<LoanRepayment> scheduled = repaymentRepo
                .findByAccountIdAndRepaymentStatus(account.getAccountId(), LoanRepaymentStatus.SCHEDULED);

        if (scheduled.isEmpty()) {
            log.warn("[Repayment] 無待繳期數，略過 accountId={}", account.getAccountId());
            return;
        }

        LoanRepayment current = scheduled.stream()
                .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                .get();

        // 標記本期已繳
        current.setRepaymentStatus(LoanRepaymentStatus.PAID);
        current.setPaidDate(LocalDate.now());
        current.setUpdateTime(LocalDateTime.now());
        repaymentRepo.save(current);

        // 更新帳戶還款進度
        account.setPaidPeriods(account.getPaidPeriods() + 1);
        account.setRemainingPrincipal(current.getRemainingAfter());

        // 下次應繳日 = 剩餘 SCHEDULED 中最小的 scheduledDate；全繳完則 null
        account.setNextPaymentDate(
                scheduled.stream()
                        .filter(r -> r.getPeriodIndex() > current.getPeriodIndex())
                        .min(Comparator.comparing(LoanRepayment::getPeriodIndex))
                        .map(LoanRepayment::getScheduledDate)
                        .orElse(null)
        );

        // 全數繳清：帳戶 → PAID_OFF，申請單 → CLOSED
        if (account.getPaidPeriods().equals(account.getConfirmedPeriod())) {
            account.setAccountStatus(LoanAccountStatus.PAID_OFF);
            log.info("[Repayment] 全數繳清 accountId={}", account.getAccountId());

            loanApplicationRepo.findById(account.getApplicationId()).ifPresent(loan -> {
                loan.setApplicationStatus(LoanApplicationStatus.CLOSED);
                loan.setUpdateTime(LocalDateTime.now());
                loanApplicationRepo.save(loan);
                log.info("[Repayment] 申請單已關閉 applicationId={}", loan.getApplicationId());
            });
        }

        account.setUpdateTime(LocalDateTime.now());
        loanAccountRepo.save(account);

        log.info("[Repayment] 進度更新完成 accountId={} period={}/{}",
                account.getAccountId(), account.getPaidPeriods(), account.getConfirmedPeriod());
    }

    // ===查詢===

    // 依帳戶 ID 查完整還款時間表，按期數升序（行員 / 客戶端共用）
    @Transactional(readOnly = true)
    public List<LoanRepaymentResponseDTO> getByAccountId(String accountId) {
        return repaymentRepo.findByAccountIdOrderByPeriodIndexAsc(accountId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ===DTO 轉換===
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

    // 產生還款 ID（前綴 + 32 碼 UUID hex），避免批次建檔時主鍵碰撞
    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
