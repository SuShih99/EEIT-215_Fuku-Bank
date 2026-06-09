package com.javaeasybank.loan.service;

/**
 * 程式說明：
 * - 貸款帳戶業務服務。
 * - 負責建立貸款帳戶、查詢帳戶與串接帳務模組，承接核准後的放款流程。
 */

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.loan.dto.response.LoanAccountResponseDTO;
import com.javaeasybank.loan.entity.LoanAccount;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanReviewDetail;
import com.javaeasybank.loan.enums.LoanAccountStatus;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import com.javaeasybank.loan.utils.AmortizationCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 貸款帳戶業務邏輯 Service
@Slf4j
/**
 * 貸款帳戶服務。
 * 在撥款完成後建立貸款帳戶與還款排程，並提供客戶端/行員端帳戶查詢資料。
 */
@Service
@Transactional
public class LoanAccountService {

    @Autowired
    private LoanAccountRepository loanAccountRepo;

    @Autowired
    private LoanApplicationRepository loanApplicationRepo;

    @Autowired
    private LoanReviewDetailRepository reviewDetailRepo;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private LoanRepaymentService loanRepaymentService;

    // 撥款完成後建立貸款帳戶（不帶貸款帳號的多載版本）
    public void createOnDisbursement(String applicationId) {
        createOnDisbursement(applicationId, null);
    }

    // 撥款完成後建立貸款帳戶（主要邏輯）
    public void createOnDisbursement(String applicationId, String loanAccountNumber) {

        // 冪等保護：重複回調時不重複建帳
        if (loanAccountRepo.findByApplicationId(applicationId).isPresent()) {
            log.warn("[Disbursement] 帳戶已存在，略過建立 applicationId={}", applicationId);
            return;
        }

        log.info("[Disbursement] Step-A 查詢申請與填單 applicationId={}", applicationId);
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("找不到二次填單：" + applicationId));

        BigDecimal principal  = detail.getConfirmedAmount().setScale(0, RoundingMode.HALF_UP);
        Integer    periods    = detail.getConfirmedPeriod();
        BigDecimal annualRate = detail.getConfirmedRate();
        log.info("[Disbursement] Step-B 計算月付金 principal={} annualRate={} periods={}",
                principal, annualRate, periods);
        BigDecimal monthlyPmt = AmortizationCalculator.calcMonthlyPayment(principal, annualRate, periods);
        log.info("[Disbursement] Step-B 完成 monthlyPmt={}", monthlyPmt);

        LocalDate startDate = LocalDate.now();

        if (loanAccountNumber == null || loanAccountNumber.isBlank()) {
            throw new BusinessException("缺少帳戶模組貸款帳號：" + applicationId);
        }
        String normalizedLoanAccountNumber = loanAccountNumber.trim();

        LoanAccount account = new LoanAccount();
        account.setAccountId(normalizedLoanAccountNumber);
        account.setAccountNumber(normalizedLoanAccountNumber);
        account.setApplicationId(applicationId);
        account.setCustomerId(loan.getCustomerId());
        account.setApplyType(loan.getApplyType());
        account.setPrincipalAmount(principal.longValue());
        account.setConfirmedPeriod(periods);
        account.setRate(annualRate);
        account.setMonthlyPayment(monthlyPmt);
        account.setPaidPeriods(0);
        account.setRemainingPrincipal(principal);
        account.setStartDate(startDate);
        account.setNextPaymentDate(startDate.plusMonths(1));
        account.setAccountStatus(LoanAccountStatus.ACTIVE);
        account.setCreateTime(LocalDateTime.now());

        log.info("[Disbursement] Step-C 儲存 LoanAccount accountId={}", account.getAccountId());
        loanAccountRepo.save(account);
        log.info("[Disbursement] Step-C 完成 applicationId={}", applicationId);

        log.info("[Disbursement] Step-D 預排還款明細 periods={}", periods);
        loanRepaymentService.createSchedule(account);
        log.info("[Disbursement] Step-D 完成 applicationId={}", applicationId);
    }

    // 查詢客戶自己的所有貸款帳戶，按建立時間降序排列
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getMyAccounts(String customerId) {
        return loanAccountRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 行員端查詢全部貸款帳戶，支援依帳戶狀態篩選
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getAllAccounts(LoanAccountStatus status) {
        List<LoanAccount> accounts = (status != null)
                ? loanAccountRepo.findByAccountStatusOrderByCreateTimeDesc(status)
                : loanAccountRepo.findAll();
        return accounts.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    // 依貸款申請編號查詢單筆帳戶，供客戶確認撥款結果使用
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getByApplicationId(String applicationId) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("此申請尚未建立貸款帳戶：" + applicationId));
        return toResponseDTO(account);
    }

    // 依帳戶 ID 查詢單筆帳戶，供 Controller 層進行所有權驗證
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getAccountById(String accountId) {
        LoanAccount account = loanAccountRepo.findById(accountId)
                .orElseThrow(() -> new BusinessException("找不到貸款帳戶：" + accountId));
        return toResponseDTO(account);
    }

    // 將 LoanAccount Entity 轉換為 LoanAccountResponseDTO
    private LoanAccountResponseDTO toResponseDTO(LoanAccount account) {
        LoanAccountResponseDTO dto = new LoanAccountResponseDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setApplicationId(account.getApplicationId());
        dto.setCustomerId(account.getCustomerId());
        customerProfileRepository.findById(account.getCustomerId()).ifPresent(p -> {
            dto.setCif(p.getCif());
            dto.setMemberName(p.getName());
        });
        dto.setApplyType(account.getApplyType());
        dto.setPrincipalAmount(account.getPrincipalAmount());
        dto.setConfirmedPeriod(account.getConfirmedPeriod());
        dto.setRate(account.getRate());
        dto.setMonthlyPayment(account.getMonthlyPayment());
        dto.setPaidPeriods(account.getPaidPeriods());
        dto.setRemainingPrincipal(account.getRemainingPrincipal());
        dto.setStartDate(account.getStartDate());
        dto.setNextPaymentDate(account.getNextPaymentDate());
        dto.setAccountStatus(account.getAccountStatus());
        dto.setCreateTime(account.getCreateTime());
        return dto;
    }
}
