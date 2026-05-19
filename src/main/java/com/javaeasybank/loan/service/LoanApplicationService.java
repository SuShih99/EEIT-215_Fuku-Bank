package com.javaeasybank.loan.service;

import com.javaeasybank.account.dto.request.LoanAccountCreateRequest;
import com.javaeasybank.account.dto.request.LoanDisbursementRequest;
import com.javaeasybank.account.dto.response.LoanAccountResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.customer.service.CustomerService;
import com.javaeasybank.customer.service.CustomerServiceImpl;
import com.javaeasybank.loan.client.LoanRiskClient;
import com.javaeasybank.loan.dto.requests.*;
import com.javaeasybank.loan.dto.response.LoanApplicationResponseDTO;
import com.javaeasybank.loan.dto.response.LoanContactLogResponseDTO;
import com.javaeasybank.loan.dto.response.LoanReviewDetailResponseDTO;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanContactLog;
import com.javaeasybank.loan.entity.LoanReviewDetail;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import com.javaeasybank.loan.enums.LoanReviewStatus;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanContactLogRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 貸款申請核心業務邏輯 Service。
 *
 * <p>本 Service 統籌貸款申請完整生命週期，涵蓋以下四大業務面向：</p>
 * <ol>
 *   <li><b>申請建立與查詢</b>：客戶送出申請（{@code insertMember}），初始狀態為
 *       {@code PENDING_CONTACT}；提供依狀態、依客戶 ID 的查詢方法。</li>
 *   <li><b>聯繫紀錄管理</b>：行員新增聯繫紀錄（{@code addContactLog}），同步更新主表
 *       {@code latestContactStatus} 與 {@code applicationStatus}。</li>
 *   <li><b>二次填單送審</b>：行員儲存草稿（{@code saveReviewDetail}）並送審
 *       （{@code submitReview}），主表推進至 {@code PENDING_REVIEW}，
 *       事務提交後觸發 {@code LoanRiskClient.submitForReview}。</li>
 *   <li><b>外部回調處理</b>：接收風控（RISK）與帳戶（ACCOUNT）模組的狀態更新回調
 *       （{@code handleStatusCallback}），驅動自動撥款流程。</li>
 * </ol>
 *
 * <p><b>事務設計重點：</b></p>
 * <ul>
 *   <li>class 層級標注 {@code @Transactional}，所有公開方法預設加入呼叫端事務。</li>
 *   <li>{@code autoDisburse} 使用 {@code NOT_SUPPORTED}，使建帳與撥款各自以獨立事務提交，
 *       避免鎖定順序死鎖。</li>
 *   <li>{@code handleAccountDisbursedCallback} 使用 {@code REQUIRES_NEW}，
 *       確保帳戶回調在獨立事務中執行。</li>
 *   <li>外部 HTTP 呼叫（風控、Email）均透過 {@code TransactionSynchronizationManager}
 *       的 {@code afterCommit} 延遲執行，防止主事務回滾後外部副作用已產生。</li>
 * </ul>
 *
 * <p><b>JDBC 直接寫入說明：</b>
 * {@code insertContactLog}、{@code updateLoanContactState}、{@code insertReviewDetail}、
 * {@code updateReviewDetail} 採用 {@code JdbcTemplate} 直接操作，
 * 繞過 JPA 一級快取，確保批次操作與 DB 結果一致。</p>
 *
 * <p><b>自身 Proxy（{@code LAService}）：</b>
 * 以 {@code @Lazy} 注入自身代理，讓 {@code autoDisburse()} 的
 * {@code @Transactional(NOT_SUPPORTED)} 能被 Spring AOP 正確攔截，
 * 避免內部呼叫繞過代理導致事務傳播失效。</p>
 */
@Slf4j
@Service
@Transactional
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository laRepo;

    @Autowired
    private LoanContactLogRepository contactLogRepo;

    @Autowired
    private LoanReviewDetailRepository reviewDetailRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LoanRiskClient loanRiskClient;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoanAccountService loanAccountService;

    @Autowired
    private AccountIntegrationService accountIntegrationService;

    /**
     * 自身 Proxy 注入，讓 {@code autoDisburse()} 的 {@code @Transactional(NOT_SUPPORTED)}
     * 能被 Spring AOP 攔截，避免內部直接呼叫繞過代理。
     */
    @Lazy
    @Autowired
    private LoanApplicationService LAService;

    @Autowired
    private EmailService emailService;

    // ── 查詢 ─────────────────────────────────────────────────────────

    /**
     * 依申請狀態查詢貸款申請清單，依建立時間降序排列（行員後台用）。
     *
     * @param status 申請狀態篩選條件
     * @return 符合指定狀態的申請清單
     */
    public List<LoanApplicationResponseDTO> getByStatus(LoanApplicationStatus status) {
        return laRepo.findByApplicationStatusOrderByCreateTimeDesc(status)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查詢當前登入客戶的所有貸款申請，依建立時間降序排列。
     *
     * @param customerId 客戶內部識別碼（從 JWT 取得）
     * @return 該客戶的所有申請清單
     */
    public List<LoanApplicationResponseDTO> getMyApplications(String customerId) {
        return laRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── 新增 ─────────────────────────────────────────────────────────

    /**
     * 客戶送出貸款申請（會員端入口）。
     *
     * <p>建立 {@code LoanApplication}，初始狀態為 {@code PENDING_CONTACT}，
     * 並以 best-effort 方式發送「申請成立」Email 通知客戶。</p>
     *
     * @param customerId 申請者的客戶識別碼（從 JWT 取得）
     * @param dto        申請內容（類型、金額、期數、利率、撥款帳號）
     * @return 新建申請的識別碼
     */
    public String insertMember(String customerId, LoanMemberRequestDTO dto) {
        LoanApplication loan = buildBaseLoan();
        loan.setCustomerId(customerId);
        fillLoanContent(loan, dto.getApplyType(), dto.getApplyAmount(),
                dto.getApplyPeriod(), dto.getRate());
        loan.setDisbursementAccount(dto.getDisbursementAccount()); // 儲存撥款入帳帳號
        entityManager.persist(loan);

        // 申請成立通知（best-effort，無 email 時僅記錄警告，不中斷流程）
        String email = customerService.findEmailByCustomerId(customerId);
        if (email != null) {
            emailService.sendLoanAppliedNotification(
                    email, loan.getApplicationId(), loan.getApplyType(),
                    loan.getApplyAmount(), loan.getApplyPeriod());
        } else {
            log.warn("[LoanApplied] 客戶無 email，略過通知。customerId={}", customerId);
        }

        return loan.getApplicationId();
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    /**
     * 產生格式化識別碼：前綴 + {@code yyyyMMddHHmmss} + 4 位隨機數字。
     *
     * @param prefix 識別碼前綴，例如 {@code "LA"}、{@code "CL"}、{@code "RD"}
     * @return 格式化的唯一識別碼
     */
    private String generateId(String prefix) {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    /**
     * 產生基礎 {@code LoanApplication} 物件，設定識別碼、初始狀態與建立時間。
     *
     * <p>初始狀態固定為 {@code PENDING_CONTACT}，等待行員進行聯繫。</p>
     *
     * @return 填入識別碼、狀態與時間戳的 {@code LoanApplication}（尚未持久化）
     */
    private LoanApplication buildBaseLoan() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationId(generateId("LA"));
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_CONTACT);
        loan.setCreateTime(LocalDateTime.now());
        return loan;
    }

    /**
     * 填入申請的業務內容欄位（貸款類型、金額、期數、利率）。
     *
     * @param loan        目標 {@code LoanApplication} 物件
     * @param applyType   貸款類型字串（例如 {@code "PERSONAL"}、{@code "HOUSE"}）
     * @param applyAmount 申請金額
     * @param applyPeriod 申請期數（月）
     * @param rate        申請利率（年利率，例如 {@code 0.04} 代表 4%）
     */
    private void fillLoanContent(LoanApplication loan, String applyType,
                                 BigDecimal applyAmount, Integer applyPeriod,
                                 BigDecimal rate) {
        loan.setApplyType(applyType);
        loan.setApplyAmount(applyAmount);
        loan.setApplyPeriod(applyPeriod);
        loan.setRate(rate);
    }

    // ── 聯繫紀錄 ─────────────────────────────────────────────────────

    /**
     * 行員新增聯繫紀錄，並同步更新主表的最新聯繫狀態。
     *
     * <p>狀態推進規則：</p>
     * <ul>
     *   <li>主表為 {@code PENDING_CONTACT} 時，首次聯繫推進為 {@code IN_CONTACT}。</li>
     *   <li>客戶表示放棄（{@code contactStatus = DECLINED}）時，主表推進為 {@code CANCELLED}。</li>
     *   <li>其他情況僅更新 {@code latestContactStatus} 與 {@code latestContactTime}，主表狀態不變。</li>
     * </ul>
     *
     * @param applicationId 申請識別碼
     * @param dto           聯繫紀錄內容（行員 ID、聯繫狀態、聯繫管道、備註）
     * @throws BusinessException 若申請不存在
     */
    public void addContactLog(String applicationId, LoanContactLogRequestDTO dto) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        LoanContactStatus contactStatus = LoanContactStatus.valueOf(dto.getContactStatus());
        LoanContactChannel contactChannel = LoanContactChannel.valueOf(dto.getContactChannel());

        LocalDateTime contactTime = LocalDateTime.now();
        insertContactLog(applicationId, dto, contactStatus, contactChannel, contactTime);

        // 若主表仍是 PENDING_CONTACT，推進為 IN_CONTACT
        LoanApplicationStatus nextApplicationStatus = null;
        if (loan.getApplicationStatus() == LoanApplicationStatus.PENDING_CONTACT) {
            nextApplicationStatus = LoanApplicationStatus.IN_CONTACT;
        }
        // 客戶放棄時，主表推進為 CANCELLED
        if (contactStatus == LoanContactStatus.DECLINED) {
            nextApplicationStatus = LoanApplicationStatus.CANCELLED;
        }

        updateLoanContactState(applicationId, contactStatus, contactTime, nextApplicationStatus);
    }

    /**
     * 透過 {@code JdbcTemplate} 直接寫入 {@code loan_contact_log} 表。
     *
     * <p>繞過 JPA 一級快取，確保寫入結果即時反映於 DB，
     * 識別碼以 {@code "CL"} 前綴的 {@code generateId} 產生。</p>
     *
     * @param applicationId  申請識別碼
     * @param dto            聯繫紀錄請求 DTO
     * @param contactStatus  聯繫結果列舉
     * @param contactChannel 聯繫管道列舉
     * @param contactTime    聯繫時間
     */
    private void insertContactLog(String applicationId, LoanContactLogRequestDTO dto,
                                  LoanContactStatus contactStatus,
                                  LoanContactChannel contactChannel,
                                  LocalDateTime contactTime) {
        jdbcTemplate.update("""
                INSERT INTO loan_contact_log
                    (log_id, application_id, emp_id, contact_status, contact_channel, contact_time, note)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """,
                generateId("CL"),
                applicationId,
                dto.getEmpId(),
                contactStatus.name(),
                contactChannel.name(),
                contactTime,
                dto.getNote());
    }

    /**
     * 透過 {@code JdbcTemplate} 直接更新 {@code loan_application} 主表的聯繫狀態快照。
     *
     * <p>若 {@code nextApplicationStatus} 為 {@code null}，
     * 僅更新 {@code latestContactStatus} 與 {@code latestContactTime}；
     * 否則同時更新 {@code applicationStatus} 與 {@code updateTime}。</p>
     *
     * @param applicationId         申請識別碼
     * @param contactStatus         最新聯繫結果
     * @param contactTime           最新聯繫時間
     * @param nextApplicationStatus 需要推進的主表狀態；若不需推進則傳入 {@code null}
     */
    private void updateLoanContactState(String applicationId,
                                        LoanContactStatus contactStatus,
                                        LocalDateTime contactTime,
                                        LoanApplicationStatus nextApplicationStatus) {
        if (nextApplicationStatus == null) {
            jdbcTemplate.update("""
                    UPDATE loan_application
                       SET latest_contact_status = ?,
                           latest_contact_time = ?
                     WHERE application_id = ?
                    """,
                    contactStatus.name(),
                    contactTime,
                    applicationId);
            return;
        }

        jdbcTemplate.update("""
                UPDATE loan_application
                   SET latest_contact_status = ?,
                       latest_contact_time = ?,
                       application_status = ?,
                       update_time = ?
                 WHERE application_id = ?
                """,
                contactStatus.name(),
                contactTime,
                nextApplicationStatus.name(),
                contactTime,
                applicationId);
    }

    /**
     * 查詢指定申請的所有聯繫紀錄，依聯繫時間降序排列。
     *
     * @param applicationId 申請識別碼
     * @return 聯繫紀錄清單（最新在前）
     */
    public List<LoanContactLogResponseDTO> getContactLogs(String applicationId) {
        return contactLogRepo.findByApplicationIdOrderByContactTimeDesc(applicationId)
                .stream()
                .map(this::toContactLogResponseDTO)
                .collect(Collectors.toList());
    }

    // ── 二次填單 ─────────────────────────────────────────────────────

    /**
     * 儲存或覆蓋行員二次填單草稿。
     *
     * <p>儲存規則：</p>
     * <ul>
     *   <li>若該申請尚無草稿，建立新的 {@code LoanReviewDetail}（狀態為 {@code DRAFT}）。</li>
     *   <li>若已有草稿（{@code DRAFT}），覆蓋舊草稿內容。</li>
     *   <li>草稿一旦送審（狀態為 {@code SUBMITTED}），不可再修改。</li>
     * </ul>
     *
     * @param applicationId 申請識別碼
     * @param dto           二次填單內容（核准金額、期數、利率、擔保品備註、行員 ID）
     * @throws BusinessException 若申請不存在，或填單已送審不可修改
     */
    public void saveReviewDetail(String applicationId, LoanReviewDetailRequestDTO dto) {

        // 確認主表存在
        laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 沒有草稿就建新的
        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElse(null);
        boolean newDetail = detail == null;

        // 已送審的填單不可再修改
        if (detail != null && detail.getReviewStatus() == LoanReviewStatus.SUBMITTED) {
            throw new BusinessException("此申請已送審，無法修改填單內容");
        }

        LocalDateTime reviewTime = LocalDateTime.now();

        if (newDetail) {
            insertReviewDetail(applicationId, dto, reviewTime);
        } else {
            updateReviewDetail(detail.getReviewId(), dto, reviewTime);
        }
    }

    /**
     * 透過 {@code JdbcTemplate} 直接寫入 {@code loan_review_detail} 表（新建草稿）。
     *
     * <p>識別碼以 {@code "RD"} 前綴的 {@code generateId} 產生；
     * {@code review_status} 固定寫入 {@code DRAFT}；
     * {@code submitted_time} 與 {@code review_note} 初始為 {@code NULL}。</p>
     *
     * @param applicationId 申請識別碼
     * @param dto           二次填單內容
     * @param reviewTime    填單操作時間
     */
    private void insertReviewDetail(String applicationId, LoanReviewDetailRequestDTO dto, LocalDateTime reviewTime) {
        jdbcTemplate.update("""
                INSERT INTO loan_review_detail
                    (review_id, application_id, confirmed_amount, confirmed_period, confirmed_rate,
                     collateral_note, emp_id, review_time, review_status, submitted_time, review_note)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)
                """,
                generateId("RD"),
                applicationId,
                dto.getConfirmedAmount(),
                dto.getConfirmedPeriod(),
                dto.getConfirmedRate(),
                dto.getCollateralNote(),
                dto.getEmpId(),
                reviewTime,
                LoanReviewStatus.DRAFT.name());
    }

    /**
     * 透過 {@code JdbcTemplate} 直接更新 {@code loan_review_detail} 草稿內容。
     *
     * <p>{@code review_status} 重置為 {@code DRAFT}，以防止誤操作覆蓋已送審填單
     * （實際上在 {@code saveReviewDetail} 已提前攔截）。</p>
     *
     * @param reviewId   填單識別碼
     * @param dto        更新後的填單內容
     * @param reviewTime 更新操作時間
     */
    private void updateReviewDetail(String reviewId, LoanReviewDetailRequestDTO dto, LocalDateTime reviewTime) {
        jdbcTemplate.update("""
                UPDATE loan_review_detail
                   SET confirmed_amount = ?,
                       confirmed_period = ?,
                       confirmed_rate = ?,
                       collateral_note = ?,
                       emp_id = ?,
                       review_time = ?,
                       review_status = ?
                 WHERE review_id = ?
                """,
                dto.getConfirmedAmount(),
                dto.getConfirmedPeriod(),
                dto.getConfirmedRate(),
                dto.getCollateralNote(),
                dto.getEmpId(),
                reviewTime,
                LoanReviewStatus.DRAFT.name(),
                reviewId);
    }

    /**
     * 行員正式送審：將填單狀態從 {@code DRAFT} 推進至 {@code SUBMITTED}，
     * 主表推進至 {@code PENDING_REVIEW}，事務提交後呼叫 {@code LoanRiskClient.submitForReview}。
     *
     * <p>前置條件：</p>
     * <ul>
     *   <li>申請狀態必須為 {@code IN_CONTACT}（已完成行員聯繫）。</li>
     *   <li>必須已建立草稿，且草稿狀態為 {@code DRAFT}（未曾送審）。</li>
     * </ul>
     *
     * <p>外部呼叫策略：風控請求在 {@code afterCommit} 執行，確保主表狀態寫入後
     * 才送出，避免風控回調比主事務更早到達。
     * 若 {@code afterCommit} 中風控呼叫失敗，行員可透過
     * {@code PATCH /api/admin/loan-applications/{id}/risk/retry} 手動重送。</p>
     *
     * @param applicationId 申請識別碼
     * @throws BusinessException 若前置狀態不符、草稿不存在或已送審
     */
    @Transactional
    public void submitReview(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 狀態前置檢查：必須是 IN_CONTACT
        if (loan.getApplicationStatus() != LoanApplicationStatus.IN_CONTACT) {
            throw new BusinessException("此申請目前狀態無法送審");
        }

        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("尚未建立二次填單草稿，無法送審"));

        // 確認目前是草稿才能送審
        if (detail.getReviewStatus() != LoanReviewStatus.DRAFT) {
            throw new BusinessException("此申請已送審，請勿重複操作");
        }

        // 更新填單狀態 DRAFT → SUBMITTED
        detail.setReviewStatus(LoanReviewStatus.SUBMITTED);
        detail.setSubmittedTime(LocalDateTime.now());

        // 同步更新主表狀態 IN_CONTACT → PENDING_REVIEW
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_REVIEW);
        loan.setUpdateTime(detail.getSubmittedTime());

        LoanRiskRequestDTO riskDto = buildRiskRequest(loan, detail);

        // 事務提交後發送風控請求（afterCommit），避免風控回調在主事務提交前抵達
        // 注意：生產環境建議改用 ApplicationEventPublisher +
        //       @TransactionalEventListener(phase = AFTER_COMMIT) 以獲得更可靠的保證
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("[SubmitReview] 事務提交，發送風控請求: {}", applicationId);
                try {
                    loanRiskClient.submitForReview(riskDto);
                } catch (Exception e) {
                    log.error("[SubmitReview] 風控請求發送失敗，appId={}, error={}", applicationId, e.getMessage());
                    // 補償路徑：行員可呼叫 PATCH /api/admin/loan-applications/{id}/risk/retry 手動重送
                    // 生產環境建議改為寫入補傳表 + 定時排程重試
                }
            }
        });
    }

    /**
     * 組裝送審至風控模組所需的 {@code LoanRiskRequestDTO}。
     *
     * <p>結合 {@code LoanApplication} 申請資訊與 {@code LoanReviewDetail} 核准條件，
     * 並查詢 {@code CustomerProfile} 補入 {@code cif} 供風控系統對照顯示。</p>
     *
     * @param loan   貸款申請 Entity
     * @param detail 二次填單 Entity（已確認為 SUBMITTED 狀態）
     * @return 完整的風控審核請求 DTO
     */
    private LoanRiskRequestDTO buildRiskRequest(LoanApplication loan, LoanReviewDetail detail) {
        LoanRiskRequestDTO dto = new LoanRiskRequestDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 補入 cif 供風控模組對照顯示用
        String cif = customerProfileRepository.findById(loan.getCustomerId())
                .map(p -> p.getCif())
                .orElse(null);

        // 風控必填欄位
        dto.setScene("LOAN_APPLY");
        dto.setBusinessId(loan.getApplicationId());
        dto.setAmount(detail.getConfirmedAmount());

        dto.setCif(cif);
        dto.setApplyType(loan.getApplyType());
        dto.setConfirmedAmount(detail.getConfirmedAmount());
        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
        dto.setConfirmedRate(detail.getConfirmedRate());
        dto.setCollateralNote(detail.getCollateralNote());
        dto.setEmpId(detail.getEmpId());
        dto.setSubmittedTime(detail.getSubmittedTime());
        return dto;
    }

    // ── 外部模組回調 ─────────────────────────────────────────────────

    /**
     * 處理外部模組的申請狀態更新回調。
     *
     * <p>依 {@code callerModule} 分流：</p>
     * <ul>
     *   <li><b>RISK（風控）</b>：
     *     <ul>
     *       <li>{@code RETURNED}（退回補件）：清空 {@code documentsSubmittedAt} 以重開補件視窗，
     *           發送補件通知 Email，不變更主表狀態。</li>
     *       <li>{@code REJECTED}（拒絕）：主表推進 {@code REJECTED}，{@code afterCommit} 發送拒絕通知。</li>
     *       <li>{@code APPROVED}（核准）：主表推進 {@code APPROVED}，{@code afterCommit} 觸發
     *           {@code autoDisburse} 自動撥款。</li>
     *     </ul>
     *   </li>
     *   <li><b>ACCOUNT（帳戶）</b>：前置狀態必須為 {@code APPROVED}，目標狀態必須為 {@code DISBURSED}；
     *       主表推進 {@code DISBURSED}，並呼叫 {@code LoanAccountService.createOnDisbursement}
     *       建立貸款帳戶，最後發送核准暨撥款通知 Email。</li>
     * </ul>
     *
     * @param applicationId 申請識別碼
     * @param dto           回調資料（包含目標狀態、呼叫方模組、貸款帳號等）
     * @throws BusinessException 若申請不存在、前置狀態不符或目標狀態不合法
     */
    public void handleStatusCallback(String applicationId, LoanStatusCallbackRequestDTO dto) {
        log.info("[StatusCallback] 收到回調 applicationId={}, caller={}, newStatus={}",
                applicationId, dto.getCallerModule(), dto.getNewStatus());
        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        String caller = dto.getCallerModule();

        if ("RISK".equals(caller)) {

            // 攔截風控傳過來的「退回補件」通知
            if (dto.getNewStatus() == LoanApplicationStatus.RETURNED) {

                // 狀態不變，依然維持在審核中
                log.info("[RiskCallback] 收到風控退回補件通知。保持狀態為 PENDING_REVIEW，觸發客戶郵件通知。applicationId={}", applicationId);

                // 清空先前的送出時間，使前端網銀的「補件上傳按鈕」重新開放
                loan.setDocumentsSubmittedAt(null);
                loan.setUpdateTime(LocalDateTime.now());
                laRepo.save(loan);

                String email = customerService.findEmailByCustomerId(loan.getCustomerId());
                log.info("[LoanCallback] 準備發送補件通知 email={}, applicationId={}", email, loan.getApplicationId());
                if (email != null) {
                    emailService.sendLoanDocumentRequiredNotification(
                            email, loan.getApplicationId(), loan.getApplyType(), loan.getApplyAmount());
                } else {
                    log.warn("[LoanCallback] 客戶無 email，略過通知。customerId={}", loan.getCustomerId());
                }
                return; // 處理完補件通知，直接結束
            }

            // 前置狀態驗證：RISK 回調時主表必須是 PENDING_REVIEW
            if (loan.getApplicationStatus() != LoanApplicationStatus.PENDING_REVIEW) {
                throw new BusinessException(
                        "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用風控回調");
            }
            // 目標狀態限制：RISK 只允許 APPROVED / REJECTED
            if (dto.getNewStatus() != LoanApplicationStatus.APPROVED
                    && dto.getNewStatus() != LoanApplicationStatus.REJECTED) {
                throw new BusinessException("風控回調目標狀態不合法：" + dto.getNewStatus());
            }

            // 拒絕：事務提交後發送拒絕通知 Email（提前取值，避免主表寫入後狀態已變）
            if (dto.getNewStatus() == LoanApplicationStatus.REJECTED) {
                final String rejectEmail = customerService.findEmailByCustomerId(loan.getCustomerId());
                final String rejectAppId = loan.getApplicationId();
                final String rejectType  = loan.getApplyType();
                final BigDecimal rejectAmt = loan.getApplyAmount();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (rejectEmail != null) {
                            emailService.sendLoanRejectedNotification(
                                    rejectEmail, rejectAppId, rejectType, rejectAmt);
                        } else {
                            log.warn("[LoanRejected] 客戶無 email，略過通知。applicationId={}", rejectAppId);
                        }
                    }
                });
            }

            // 核准：事務提交後觸發 autoDisburse 自動建帳與撥款
            if (dto.getNewStatus() == LoanApplicationStatus.APPROVED) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("[AutoDisburse] 風控核准，觸發自動撥款 applicationId={}", applicationId);
                        try {
                            LAService.autoDisburse(applicationId);
                        } catch (Exception e) {
                            log.error("[AutoDisburse] 自動撥款失敗，申請保留 APPROVED 供重試 applicationId={}",
                                    applicationId, e);
                        }
                    }
                });
            }

        } else if ("ACCOUNT".equals(caller)) {

            // 前置狀態驗證：ACCOUNT 回調時主表必須是 APPROVED
            if (loan.getApplicationStatus() != LoanApplicationStatus.APPROVED) {
                throw new BusinessException(
                        "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用帳戶撥款回調");
            }
            // 目標狀態限制：ACCOUNT 只允許 DISBURSED
            if (dto.getNewStatus() != LoanApplicationStatus.DISBURSED) {
                throw new BusinessException("帳戶回調目標狀態不合法：" + dto.getNewStatus());
            }

        } else {
            throw new BusinessException("不認識的 callerModule：" + caller);
        }

        // 更新主表狀態
        loan.setApplicationStatus(dto.getNewStatus());
        loan.setUpdateTime(LocalDateTime.now());
        laRepo.save(loan);

        // ACCOUNT 模組撥款確認後，同步建立貸款帳戶並發送核准暨撥款通知
        if ("ACCOUNT".equals(caller)) {
            loanAccountService.createOnDisbursement(applicationId, dto.getLoanAccountNumber());

            try {
                LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                        .orElse(null);
                String disbEmail = customerService.findEmailByCustomerId(loan.getCustomerId());
                if (disbEmail != null && detail != null) {
                    var loanAccount = loanAccountService.getByApplicationId(applicationId);
                    String firstPaymentDate = loanAccount.getNextPaymentDate() != null
                            ? loanAccount.getNextPaymentDate().toString()
                            : null;
                    emailService.sendLoanApprovedAndDisbursedNotification(
                            disbEmail,
                            applicationId,
                            loan.getApplyType(),
                            detail.getConfirmedAmount(),
                            detail.getConfirmedPeriod(),
                            detail.getConfirmedRate(),
                            loanAccount.getAccountId(),
                            loan.getDisbursementAccount(),
                            firstPaymentDate);
                } else {
                    log.warn("[LoanDisbursed] 略過通知：email={} detail={} applicationId={}",
                            disbEmail, detail, applicationId);
                }
            } catch (Exception e) {
                log.error("[LoanDisbursed] 發送核准暨撥款通知失敗，applicationId={}", applicationId, e);
            }
        }
    }

    /**
     * 帳戶撥款完成回調的簡化入口（不帶貸款帳號）。
     *
     * <p>委派至 {@link #handleAccountDisbursedCallback(String, String)}，
     * {@code loanAccountNumber} 傳入 {@code null}。
     * 以 {@code REQUIRES_NEW} 傳播在獨立事務中執行。</p>
     *
     * @param applicationId 申請識別碼
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAccountDisbursedCallback(String applicationId) {
        handleAccountDisbursedCallback(applicationId, null);
    }

    /**
     * 帳戶撥款完成回調（帶貸款帳號版本，主要邏輯）。
     *
     * <p>組裝 {@code LoanStatusCallbackRequestDTO}（{@code callerModule = "ACCOUNT"}，
     * {@code newStatus = DISBURSED}），委派至 {@code handleStatusCallback} 執行狀態轉移。
     * 以 {@code REQUIRES_NEW} 傳播確保在獨立事務中執行，
     * 避免被外層事務的回滾影響。</p>
     *
     * @param applicationId     申請識別碼
     * @param loanAccountNumber 帳戶模組建立的貸款帳號（可為 {@code null}）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAccountDisbursedCallback(String applicationId, String loanAccountNumber) {
        LoanStatusCallbackRequestDTO callbackDto = new LoanStatusCallbackRequestDTO();
        callbackDto.setCallerModule("ACCOUNT");
        callbackDto.setNewStatus(LoanApplicationStatus.DISBURSED);
        callbackDto.setLoanAccountNumber(loanAccountNumber);
        callbackDto.setNote("account afterCommit: disbursement completed");
        handleStatusCallback(applicationId, callbackDto);
    }

    /**
     * 風控核准後自動建帳與撥款（由 {@code handleStatusCallback} APPROVED 分支的 {@code afterCommit} 觸發）。
     *
     * <p><b>事務傳播：{@code NOT_SUPPORTED}</b><br>
     * 覆蓋 class 層級 {@code @Transactional}，使本方法不持有任何外層事務。
     * {@code createLoanAccount} 和 {@code disburseLoan} 各自以 {@code REQUIRED}
     * 建立並提交自己的獨立事務，確保建帳提交後撥款的 {@code lockAccounts} 才能查到 LOAN 帳戶。</p>
     *
     * <p>執行步驟：</p>
     * <ol>
     *   <li>冪等保護：狀態已非 {@code APPROVED} 時直接略過（防止重複觸發）。</li>
     *   <li>步驟一：呼叫 {@code AccountIntegrationService.createLoanAccount} 建立貸款負債帳戶，
     *       取得 {@code loanAccountNumber}。</li>
     *   <li>步驟二：呼叫 {@code AccountIntegrationService.disburseLoan} 執行撥款；
     *       其 {@code afterCommit} 會呼叫 {@code handleAccountDisbursedCallback} 更新主表至 {@code DISBURSED}。</li>
     * </ol>
     *
     * @param applicationId 申請識別碼
     * @throws BusinessException 若申請不存在、撥款帳號為空或二次填單不存在
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void autoDisburse(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 冪等保護：若已不是 APPROVED（例如重複觸發），直接略過
        if (loan.getApplicationStatus() != LoanApplicationStatus.APPROVED) {
            log.warn("[AutoDisburse] 狀態非 APPROVED，略過 applicationId={} status={}",
                    applicationId, loan.getApplicationStatus());
            return;
        }

        if (loan.getDisbursementAccount() == null || loan.getDisbursementAccount().isBlank()) {
            throw new BusinessException("申請未儲存撥款帳號，無法自動撥款：" + applicationId);
        }

        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("找不到二次填單，無法自動撥款：" + applicationId));

        log.info("[AutoDisburse] 開始撥款 applicationId={} customerId={} amount={} disbursementAccount={}",
                applicationId, loan.getCustomerId(),
                detail.getConfirmedAmount(), loan.getDisbursementAccount());

        // 步驟一：在 Account 模組建立貸款負債帳戶
        LoanAccountCreateRequest createReq = new LoanAccountCreateRequest();
        createReq.setCustomerId(loan.getCustomerId());
        createReq.setLiability(detail.getConfirmedAmount());
        createReq.setRate(detail.getConfirmedRate());
        LoanAccountResponse accountResp = accountIntegrationService.createLoanAccount(createReq);
        String loanAccountNumber = accountResp.getLoanAccountNumber();
        log.info("[AutoDisburse] Step1 完成 loanAccountNumber={}", loanAccountNumber);

        // 步驟二：撥款
        // lockAccounts 將同時鎖定：909000000001（銀行）、loanAccountNumber（LOAN帳）、disbursementAccount（客戶CHECKING）
        // disburseLoan 的 afterCommit 會呼叫 handleAccountDisbursedCallback（ACCOUNT/DISBURSED）
        log.info("[AutoDisburse] Step2 開始撥款 lockAccounts 目標: 銀行=909000000001 loan={} to={}",
                loanAccountNumber, loan.getDisbursementAccount());
        LoanDisbursementRequest disburseReq = new LoanDisbursementRequest();
        disburseReq.setApplicationId(applicationId);
        disburseReq.setLoanAccountNumber(loanAccountNumber);
        disburseReq.setToAccountNumber(loan.getDisbursementAccount());
        disburseReq.setAmount(detail.getConfirmedAmount());
        disburseReq.setNote("貸款核准自動撥款 applicationId=" + applicationId);
        accountIntegrationService.disburseLoan(disburseReq);
        log.info("[AutoDisburse] Step2 完成，撥款指令送出 applicationId={}", applicationId);
    }

    /**
     * 撥款補償：行員手動重送撥款（狀態必須仍為 {@code APPROVED}）。
     *
     * <p>對應 {@code autoDisburse} 的 {@code afterCommit} 失敗時保留 {@code APPROVED} 的補救路徑。</p>
     *
     * <p>補償邏輯：</p>
     * <ul>
     *   <li>若帳戶模組已有既有撥款紀錄，改補送 {@code ACCOUNT} 回調，
     *       避免重複建帳或重複撥款。</li>
     *   <li>若無既有紀錄，透過 {@code LAService} Proxy 呼叫 {@code autoDisburse}，
     *       確保事務傳播正確。</li>
     * </ul>
     *
     * @param applicationId 申請識別碼
     * @throws BusinessException 若申請不存在或狀態不為 {@code APPROVED}
     */
    public void retryDisburse(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        if (loan.getApplicationStatus() != LoanApplicationStatus.APPROVED) {
            throw new BusinessException(
                    "此申請狀態為 " + loan.getApplicationStatus() + "，無需重送撥款（僅 APPROVED 可重送）");
        }

        // 已有撥款紀錄時，補送 ACCOUNT 回調而非重複撥款
        if (accountIntegrationService.hasDisbursementRecordByApplicationId(applicationId)) {
            log.warn("[RetryDisburse] 偵測到既有撥款紀錄，改補送 ACCOUNT 回調 applicationId={}", applicationId);
            LoanStatusCallbackRequestDTO callbackDto = new LoanStatusCallbackRequestDTO();
            callbackDto.setCallerModule("ACCOUNT");
            callbackDto.setNewStatus(LoanApplicationStatus.DISBURSED);
            callbackDto.setNote("retryDisburse: 補送 ACCOUNT 回調");
            handleStatusCallback(applicationId, callbackDto);
            return;
        }

        log.info("[RetryDisburse] 行員手動重送撥款 applicationId={}", applicationId);
        // 透過 LAService proxy 確保 autoDisburse 的 @Transactional 被 Spring AOP 攔截
        LAService.autoDisburse(applicationId);
    }

    /**
     * 風控送審補償：行員手動重送風控審核請求（狀態必須仍為 {@code PENDING_REVIEW}）。
     *
     * <p>對應 {@code submitReview} 中 {@code afterCommit} 風控呼叫失敗的補救路徑。
     * 行員確認狀態停滯於 {@code PENDING_REVIEW} 後，可呼叫此方法直接重送。</p>
     *
     * @param applicationId 申請識別碼
     * @throws BusinessException 若申請不存在、狀態不為 {@code PENDING_REVIEW} 或二次填單不存在
     */
    public void retryRiskSubmit(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        if (loan.getApplicationStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "此申請狀態為 " + loan.getApplicationStatus() + "，無需重送（僅 PENDING_REVIEW 可重送）");
        }

        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("找不到二次填單，無法重送"));

        LoanRiskRequestDTO riskDto = buildRiskRequest(loan, detail);
        log.info("[RetryRisk] 行員手動重送風控 applicationId={}", applicationId);
        loanRiskClient.submitForReview(riskDto);
    }

    /**
     * 查詢指定申請的二次填單內容。
     *
     * @param applicationId 申請識別碼
     * @return 填單資訊 DTO；若尚未建立草稿則回傳 {@code null}
     * @throws BusinessException 若申請不存在
     */
    public LoanReviewDetailResponseDTO getReviewDetail(String applicationId) {
        if (!laRepo.existsById(applicationId)) {
            throw new BusinessException("找不到申請編號：" + applicationId);
        }
        return reviewDetailRepo.findByApplicationId(applicationId)
                .map(this::toReviewDetailResponseDTO)
                .orElse(null);
    }

    /**
     * 置頂查詢：回傳曾被外部模組（風控、帳戶）異動過狀態的申請，依更新時間降序排列。
     *
     * <p>{@code updateTime} 非空表示該申請曾收到外部回調，適用於行員後台的「最近異動」列表。</p>
     *
     * @return 曾被外部模組更新狀態的申請清單（最新在前）
     */
    public List<LoanApplicationResponseDTO> getRecentlyUpdated() {
        return laRepo.findByUpdateTimeIsNotNullOrderByUpdateTimeDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── 利率規則 ─────────────────────────────────────────────────────

    /**
     * 回傳利率規則表，供前端頁面載入時取得並自行計算利率。
     *
     * <p>前端依此規則執行以下邏輯：</p>
     * <ol>
     *   <li>依貸款種類顯示合法期數下拉選單。</li>
     *   <li>選完期數後自動計算並顯示利率（{@code baseRate + termRate}）。</li>
     *   <li>送出申請時，將計算好的利率一併傳入後端。</li>
     * </ol>
     *
     * <p>各貸款種類包含 {@code baseRate}（基礎利率）、{@code periods}（合法期數清單），
     * 以及可選的 {@code fixedRate: true}（固定利率，不加期數加碼，例如學生貸款）。
     * {@code termRates} 為各期數對應的加碼利率對照表。</p>
     *
     * @return 包含 {@code types}（各種類規則）與 {@code termRates}（期數加碼對照）的規則 Map
     */
    public Map<String, Object> getRateRules() {

        // 各貸款種類：基礎利率 + 合法期數（fixedRate 為 true 表示固定利率，不加 termRate）
        Map<String, Object> types = new LinkedHashMap<>();
        types.put("PERSONAL", Map.of(
                "baseRate", new BigDecimal("0.04"),
                "periods", List.of(12, 24, 36, 48, 60)));
        types.put("CAR", Map.of(
                "baseRate", new BigDecimal("0.025"),
                "periods", List.of(12, 24, 36, 48, 60)));
        types.put("MOTOR", Map.of(
                "baseRate", new BigDecimal("0.045"),
                "periods", List.of(12, 24, 36)));
        types.put("STUDENT", Map.of(
                "baseRate", new BigDecimal("0.015"),
                "periods", List.of(60, 84, 120),
                "fixedRate", true // 固定利率，不加 termRate
        ));
        types.put("BUSINESS", Map.of(
                "baseRate", new BigDecimal("0.02"),
                "periods", List.of(36, 60, 84)));
        types.put("HOUSE", Map.of(
                "baseRate", new BigDecimal("0.018"),
                "periods", List.of(120, 240, 360, 480)));
        types.put("LAND", Map.of(
                "baseRate", new BigDecimal("0.028"),
                "periods", List.of(120, 180, 240)));

        // 期數加碼對照表
        Map<String, BigDecimal> termRates = new LinkedHashMap<>();
        termRates.put("12", BigDecimal.ZERO);
        termRates.put("24", new BigDecimal("0.002"));
        termRates.put("36", new BigDecimal("0.005"));
        termRates.put("48", new BigDecimal("0.008"));
        termRates.put("60", new BigDecimal("0.01"));
        termRates.put("84", new BigDecimal("0.015"));
        termRates.put("120", BigDecimal.ZERO);
        termRates.put("180", new BigDecimal("0.002"));
        termRates.put("240", new BigDecimal("0.004"));
        termRates.put("360", new BigDecimal("0.006"));
        termRates.put("480", new BigDecimal("0.008"));

        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("types", types);
        rules.put("termRates", termRates);

        return rules;
    }

    // ── DTO 轉換 ─────────────────────────────────────────────────────

    /**
     * 將 {@code LoanApplication} Entity 轉換為 {@code LoanApplicationResponseDTO}。
     *
     * <p>額外查詢 {@code CustomerProfile} 填入 {@code cif} 與姓名供前端顯示；
     * 同時查詢 {@code LoanReviewDetail} 填入核准條件欄位（確認金額、期數、利率），
     * 若尚未建立填單則相關欄位為 {@code null}。</p>
     *
     * @param loan 貸款申請 Entity
     * @return 對應的回應 DTO
     */
    private LoanApplicationResponseDTO toResponseDTO(LoanApplication loan) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 用 customerId 查出 cif 與姓名供前端顯示（Primary Key 查詢，效能无礙）
        customerProfileRepository.findById(loan.getCustomerId()).ifPresent(p -> {
            dto.setCif(p.getCif());
            dto.setMemberName(p.getName());
        });
        dto.setApplyType(loan.getApplyType());
        dto.setApplyAmount(loan.getApplyAmount());
        dto.setApplyPeriod(loan.getApplyPeriod());
        dto.setRate(loan.getRate());
        dto.setDisbursementAccount(loan.getDisbursementAccount());
        dto.setApplicationStatus(loan.getApplicationStatus());
        dto.setCreateTime(loan.getCreateTime());
        dto.setUpdateTime(loan.getUpdateTime());
        dto.setLatestContactStatus(loan.getLatestContactStatus());
        dto.setLatestContactTime(loan.getLatestContactTime());
        dto.setDocumentsSubmittedAt(loan.getDocumentsSubmittedAt());
        // 帶入二次填單確認值（有填單才有值，否則 null）
        reviewDetailRepo.findByApplicationId(loan.getApplicationId()).ifPresent(review -> {
            dto.setConfirmedAmount(review.getConfirmedAmount());
            dto.setConfirmedPeriod(review.getConfirmedPeriod());
            dto.setConfirmedRate(review.getConfirmedRate());
        });
        return dto;
    }

    /**
     * 將 {@code LoanContactLog} Entity 轉換為 {@code LoanContactLogResponseDTO}。
     *
     * @param log 聯繫紀錄 Entity
     * @return 對應的回應 DTO
     */
    private LoanContactLogResponseDTO toContactLogResponseDTO(LoanContactLog log) {
        LoanContactLogResponseDTO dto = new LoanContactLogResponseDTO();
        dto.setLogId(log.getLogId());
        dto.setApplicationId(log.getApplicationId());
        dto.setEmpId(log.getEmpId());
        dto.setContactStatus(log.getContactStatus());
        dto.setContactChannel(log.getContactChannel());
        dto.setContactTime(log.getContactTime());
        dto.setNote(log.getNote());
        return dto;
    }

    /**
     * 將 {@code LoanReviewDetail} Entity 轉換為 {@code LoanReviewDetailResponseDTO}。
     *
     * @param detail 二次填單 Entity
     * @return 對應的回應 DTO
     */
    private LoanReviewDetailResponseDTO toReviewDetailResponseDTO(LoanReviewDetail detail) {
        LoanReviewDetailResponseDTO dto = new LoanReviewDetailResponseDTO();
        dto.setReviewId(detail.getReviewId());
        dto.setApplicationId(detail.getApplicationId());
        dto.setConfirmedAmount(detail.getConfirmedAmount());
        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
        dto.setConfirmedRate(detail.getConfirmedRate());
        dto.setCollateralNote(detail.getCollateralNote());
        dto.setEmpId(detail.getEmpId());
        dto.setReviewTime(detail.getReviewTime());
        dto.setReviewStatus(detail.getReviewStatus());
        dto.setSubmittedTime(detail.getSubmittedTime());
        dto.setReviewNote(detail.getReviewNote());
        return dto;
    }
}
