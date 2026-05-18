package com.javaeasybank.loan.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/*
 * 負責貸款帳戶相關業務：
 *   - 撥款協調：ACCOUNT 模組確認撥款後，依二次填單資料建立 LoanAccount 紀錄
 *   - 客戶查詢：依 customerId 或 applicationId 查詢帳戶資訊
 */
@Slf4j
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

    // ===撥款協調===

    /**
     * 由 LoanApplicationService.handleStatusCallback 在 ACCOUNT 分支呼叫。
     * 讀取二次填單核准資料，建立 LoanAccount 並設定還款起算日。
     * 具備冪等保護：帳戶已存在時直接略過，不拋錯。
     */
    public void createOnDisbursement(String applicationId) {
        createOnDisbursement(applicationId, null);
    }

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

        BigDecimal principal    = detail.getConfirmedAmount();
        Integer    periods      = detail.getConfirmedPeriod();
        BigDecimal annualRate   = detail.getConfirmedRate();
        log.info("[Disbursement] Step-B 計算月付金 principal={} annualRate={} periods={}",
                principal, annualRate, periods);
        BigDecimal monthlyPmt   = AmortizationCalculator.calcMonthlyPayment(principal, annualRate, periods);
        log.info("[Disbursement] Step-B 完成 monthlyPmt={}", monthlyPmt);

        LocalDate startDate = LocalDate.now();

        LoanAccount account = new LoanAccount();
        account.setAccountId(generateId("LAC"));
        account.setAccountNumber(loanAccountNumber);
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
        // 預排 N 期還款明細
        loanRepaymentService.createSchedule(account);
        log.info("[Disbursement] Step-D 完成 applicationId={}", applicationId);
    }

    // ===客戶查詢===

    // 查詢客戶自己的所有貸款帳戶，按建立時間降序
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getMyAccounts(String customerId) {
        return loanAccountRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 行員端：查全部帳戶（可依 accountStatus 篩選；null 表示不篩選）
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getAllAccounts(LoanAccountStatus status) {
        List<LoanAccount> accounts = (status != null)
                ? loanAccountRepo.findByAccountStatusOrderByCreateTimeDesc(status)
                : loanAccountRepo.findAll();
        return accounts.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    // 依申請編號查單筆帳戶（客戶確認撥款結果用）
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getByApplicationId(String applicationId) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("此申請尚未建立貸款帳戶：" + applicationId));
        return toResponseDTO(account);
    }

    // 依帳戶 ID 查單筆（客戶端還款時間表的所有權驗證用）
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getAccountById(String accountId) {
        LoanAccount account = loanAccountRepo.findById(accountId)
                .orElseThrow(() -> new BusinessException("找不到貸款帳戶：" + accountId));
        return toResponseDTO(account);
    }

    // ===工具方法===

    // 產生格式化 ID（前綴 + yyyyMMddHHmmss + 4 位亂數），與 LoanApplicationService 同規格
    private String generateId(String prefix) {
        String timeStr       = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix  = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    // Entity → LoanAccountResponseDTO（不帶還款明細，明細由還款模組負責）
    private LoanAccountResponseDTO toResponseDTO(LoanAccount account) {
        LoanAccountResponseDTO dto = new LoanAccountResponseDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setApplicationId(account.getApplicationId());
        dto.setCustomerId(account.getCustomerId());
        String cif = customerProfileRepository.findById(account.getCustomerId())
                .map(p -> p.getCif())
                .orElse(null);
        dto.setCif(cif);
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
