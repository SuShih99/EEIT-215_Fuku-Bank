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

/**
 * 貸款帳戶業務邏輯 Service。
 *
 * <p>負責兩大業務：</p>
 * <ol>
 *   <li><b>撥款協調</b>：接收帳戶模組（ACCOUNT）的撥款完成通知後，
 *       依 {@code LoanReviewDetail} 的核准條件建立 {@code LoanAccount}，
 *       並呼叫 {@code LoanRepaymentService} 預排所有還款期數。</li>
 *   <li><b>帳戶查詢</b>：提供客戶查詢個人帳戶、行員後台查詢全部帳戶等方法。</li>
 * </ol>
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

    // ── 撥款協調 ─────────────────────────────────────────────────────

    /**
     * 撥款完成後建立貸款帳戶（不帶貸款帳號的多載版本）。
     *
     * <p>委派至 {@link #createOnDisbursement(String, String)}，
     * 帳號欄位設為 {@code null}，待後續由帳戶模組補充。</p>
     *
     * @param applicationId 貸款申請識別碼
     */
    public void createOnDisbursement(String applicationId) {
        createOnDisbursement(applicationId, null);
    }

    /**
     * 撥款完成後建立貸款帳戶（主要邏輯）。
     *
     * <p>由 {@code LoanApplicationService.handleStatusCallback} 在
     * {@code callerModule = "ACCOUNT"} 分支呼叫，執行步驟：</p>
     * <ol>
     *   <li>冪等保護：若帳戶已存在則直接略過，防止重複撥款回調建出多筆帳戶。</li>
     *   <li>讀取 {@code LoanApplication} 與 {@code LoanReviewDetail} 取得核准條件。</li>
     *   <li>以 {@code AmortizationCalculator} 計算月付金。</li>
     *   <li>建立並儲存 {@code LoanAccount}，初始狀態為 {@code ACTIVE}。</li>
     *   <li>呼叫 {@code LoanRepaymentService.createSchedule} 預排所有還款期數。</li>
     * </ol>
     *
     * @param applicationId    貸款申請識別碼
     * @param loanAccountNumber 帳戶模組產生的貸款帳號（可為 {@code null}）
     * @throws BusinessException 若申請或二次填單資料不存在
     */
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

        BigDecimal principal  = detail.getConfirmedAmount();
        Integer    periods    = detail.getConfirmedPeriod();
        BigDecimal annualRate = detail.getConfirmedRate();
        log.info("[Disbursement] Step-B 計算月付金 principal={} annualRate={} periods={}",
                principal, annualRate, periods);
        BigDecimal monthlyPmt = AmortizationCalculator.calcMonthlyPayment(principal, annualRate, periods);
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
        loanRepaymentService.createSchedule(account);
        log.info("[Disbursement] Step-D 完成 applicationId={}", applicationId);
    }

    // ── 查詢 ─────────────────────────────────────────────────────────

    /**
     * 查詢客戶自己的所有貸款帳戶，按建立時間降序排列。
     *
     * @param customerId 客戶內部識別碼
     * @return 客戶的貸款帳戶清單（不含還款明細）
     */
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getMyAccounts(String customerId) {
        return loanAccountRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 行員端查詢全部貸款帳戶，支援依帳戶狀態篩選。
     *
     * @param status 帳戶狀態篩選條件；傳入 {@code null} 表示查詢全部
     * @return 符合條件的貸款帳戶清單
     */
    @Transactional(readOnly = true)
    public List<LoanAccountResponseDTO> getAllAccounts(LoanAccountStatus status) {
        List<LoanAccount> accounts = (status != null)
                ? loanAccountRepo.findByAccountStatusOrderByCreateTimeDesc(status)
                : loanAccountRepo.findAll();
        return accounts.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * 依貸款申請編號查詢單筆帳戶，供客戶確認撥款結果使用。
     *
     * @param applicationId 貸款申請識別碼
     * @return 對應的貸款帳戶資訊
     * @throws BusinessException 若申請尚未建立帳戶（撥款未完成）
     */
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getByApplicationId(String applicationId) {
        LoanAccount account = loanAccountRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("此申請尚未建立貸款帳戶：" + applicationId));
        return toResponseDTO(account);
    }

    /**
     * 依帳戶 ID 查詢單筆帳戶，供 Controller 層進行所有權驗證。
     *
     * @param accountId 貸款帳戶識別碼
     * @return 對應的貸款帳戶資訊
     * @throws BusinessException 若帳戶不存在
     */
    @Transactional(readOnly = true)
    public LoanAccountResponseDTO getAccountById(String accountId) {
        LoanAccount account = loanAccountRepo.findById(accountId)
                .orElseThrow(() -> new BusinessException("找不到貸款帳戶：" + accountId));
        return toResponseDTO(account);
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    /**
     * 產生格式化識別碼：前綴 + {@code yyyyMMddHHmmss} + 4 位隨機數字。
     * 格式與 {@code LoanApplicationService} 的 ID 產生規格相同。
     *
     * @param prefix 識別碼前綴，例如 {@code "LAC"}
     * @return 格式化的唯一識別碼
     */
    private String generateId(String prefix) {
        String timeStr      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    /**
     * 將 {@code LoanAccount} Entity 轉換為 {@code LoanAccountResponseDTO}。
     * 不帶還款明細（明細由 {@code LoanRepaymentService} 獨立提供）。
     * 額外查詢 {@code CustomerProfile} 以填入對外顯示的 CIF 欄位。
     *
     * @param account 貸款帳戶 Entity
     * @return 對應的回應 DTO
     */
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
