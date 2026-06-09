package com.javaeasybank.loan.service;

/**
 * 程式說明：
 * - 貸款申請核心業務服務。
 * - 處理申請建立、聯絡紀錄、審核、文件補件、核准撥款、通知與狀態流轉。
 */

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
import com.javaeasybank.loan.repository.LoanDocumentRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
 * 貸款申請流程核心服務。
 * 統一處理會員申請、行員聯繫、二次填單、風控送審、審核回調、核准撥款與狀態查詢。
 */
@Slf4j
@Service
@Transactional
public class LoanApplicationService {

    private static final String BATCH_INITIAL = "INITIAL";

    @Autowired
    private LoanApplicationRepository laRepo;

    @Autowired
    private LoanContactLogRepository contactLogRepo;

    @Autowired
    private LoanReviewDetailRepository reviewDetailRepo;

    @Autowired
    private LoanDocumentRepository documentRepo;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Autowired
    private NotificationService notificationService;

    // 自身 proxy 注入：讓 autoDisburse() 的 @Transactional 能被 Spring AOP 攔截
    @Lazy
    @Autowired
    private LoanApplicationService LAService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LoanContractPdfService loanContractPdfService;
    // 依狀態顯示
    public List<LoanApplicationResponseDTO> getByStatus(LoanApplicationStatus status) {
        return laRepo.findByApplicationStatusOrderByCreateTimeDesc(status)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 查詢當前登入客戶所有申請
    public List<LoanApplicationResponseDTO> getMyApplications(String customerId) {
        return laRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /// 會員申請
    public String insertMember(String customerId, LoanMemberRequestDTO dto) {
        LoanApplication loan = buildBaseLoan();
        loan.setCustomerId(customerId);
        fillLoanContent(loan, dto.getApplyType(), dto.getApplyAmount(),
                dto.getApplyPeriod(), dto.getRate());
        loan.setDisbursementAccount(dto.getDisbursementAccount()); // 儲存撥款入帳帳號
        entityManager.persist(loan);

        // 申請成立通知
        String email = customerService.findEmailByCustomerId(customerId);
        if (email != null) {
            emailService.sendLoanAppliedNotification(
                    email, loan.getApplicationId(), loan.getApplyType(),
                    loan.getApplyAmount(), loan.getApplyPeriod());
            notificationService.createNotification(
                    customerId,
                    NotificationType.LOAN,
                    "貸款申請已受理",
                    "您的貸款申請已送出，請等待後續審核。",
                    "/user/loan-status");
        } else {
            log.warn("[LoanApplied] 客戶無 email，略過通知。customerId={}", customerId);
        }

        return loan.getApplicationId();
    }

    // 共用：產生格式化 ID（前綴 + yyyyMMddHHmmss + 4 位亂數）
    private String generateId(String prefix) {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    // 產生貸款申請物件 + 預設狀態
    private LoanApplication buildBaseLoan() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationId(generateId("LA"));
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_CONTACT);
        loan.setCreateTime(LocalDateTime.now());
        return loan;
    }

    // 共用：填申請內容
    private void fillLoanContent(LoanApplication loan, String applyType,
                                 BigDecimal applyAmount, Integer applyPeriod,
                                 BigDecimal rate) {
        loan.setApplyType(applyType);
        loan.setApplyAmount(applyAmount);
        loan.setApplyPeriod(applyPeriod);
        loan.setRate(rate);
    }
    // 新增聯繫紀錄，同步更新主表最新聯繫狀態
    public void addContactLog(String applicationId, LoanContactLogRequestDTO dto) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        LoanContactStatus contactStatus = LoanContactStatus.valueOf(dto.getContactStatus());
        LoanContactChannel contactChannel = LoanContactChannel.valueOf(dto.getContactChannel());

        LocalDateTime contactTime = LocalDateTime.now();
        LoanContactLog log = new LoanContactLog();
        log.setLogId(generateId("CL"));
        log.setApplicationId(applicationId);
        log.setEmpId(dto.getEmpId());
        log.setContactStatus(contactStatus);
        log.setContactChannel(contactChannel);
        log.setContactTime(contactTime);
        log.setNote(dto.getNote());
        contactLogRepo.save(log);

        // 若主表仍是 PENDING_CONTACT，推進為 IN_CONTACT
        if (loan.getApplicationStatus() == LoanApplicationStatus.PENDING_CONTACT) {
            loan.setApplicationStatus(LoanApplicationStatus.IN_CONTACT);
        }
        // 客戶放棄時，主表推進為 CANCELLED
        if (contactStatus == LoanContactStatus.DECLINED) {
            loan.setApplicationStatus(LoanApplicationStatus.CANCELLED);
        }

        loan.setLatestContactStatus(contactStatus);
        loan.setLatestContactTime(contactTime);
        loan.setUpdateTime(contactTime);
        laRepo.save(loan);
    }

    // 查某申請的所有聯繫紀錄
    public List<LoanContactLogResponseDTO> getContactLogs(String applicationId) {
        return contactLogRepo.findByApplicationIdOrderByContactTimeDesc(applicationId)
                .stream()
                .map(this::toContactLogResponseDTO)
                .collect(Collectors.toList());
    }
    // 儲存草稿：有草稿就覆蓋同一筆，沒有就新建
    // 送審後不可修改
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
            detail = new LoanReviewDetail();
            detail.setReviewId(generateId("RD"));
            detail.setApplicationId(applicationId);
            detail.setReviewStatus(LoanReviewStatus.DRAFT);
        } else {
            detail.setReviewStatus(LoanReviewStatus.DRAFT);
        }

        detail.setConfirmedAmount(dto.getConfirmedAmount());
        detail.setConfirmedPeriod(dto.getConfirmedPeriod());
        detail.setConfirmedRate(dto.getConfirmedRate());
        detail.setCollateralNote(dto.getCollateralNote());
        detail.setEmpId(dto.getEmpId());
        detail.setReviewTime(reviewTime);
        reviewDetailRepo.save(detail);
    }

    // 送審：草稿 → SUBMITTED，主表推進為 PENDING_REVIEW
    // 申請狀態必須是 IN_CONTACT
    @Transactional
    public void submitReview(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 狀態前置檢查
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
        // 準備送出的 DTO
        loan.setUpdateTime(detail.getSubmittedTime());

        LoanRiskRequestDTO riskDto = buildRiskRequest(loan, detail);

        // 註冊事務提交後的執行邏輯。
        // 注意：若要在生產環境確保可靠性，建議改用 ApplicationEventPublisher 並搭配
        // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

    // 送風控的DTO (申請資料+二次填單)
    private LoanRiskRequestDTO buildRiskRequest(LoanApplication loan, LoanReviewDetail detail) {
        LoanRiskRequestDTO dto = new LoanRiskRequestDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 補入 cif 供風控模組對照顯示用
        String cif = customerProfileRepository.findById(loan.getCustomerId())
                .map(p -> p.getCif())
                .orElse(null);

        // 風控必填
        dto.setScene("LOAN_APPLY");
        dto.setBusinessId(loan.getApplicationId());
        dto.setAmount(detail.getConfirmedAmount());

        dto.setCif(cif);
        dto.setApplyType(loan.getApplyType());
        dto.setConfirmedAmount(detail.getConfirmedAmount());
        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
        dto.setConfirmedRate(detail.getConfirmedRate());
        dto.setCollateralNote(detail.getCollateralNote());
        if (loan.getDocumentsSubmittedAt() != null) {
            dto.setDocuments(documentRepo
                    .findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
                            loan.getApplicationId(), BATCH_INITIAL, 0)
                    .stream()
                    .map(d -> new LoanDocumentInfoDTO(
                            d.getDocumentId(),
                            d.getDocumentType().name(),
                            d.getFileUrl(),
                            d.getOriginalName()))
                    .collect(Collectors.toList()));
        }
        dto.setEmpId(detail.getEmpId());
        dto.setSubmittedTime(detail.getSubmittedTime());
        return dto;
    }

    // 外部模組回調，更新主表狀態
    // RISK   模組：PENDING_REVIEW → APPROVED / REJECTED / RETURNED
    // ACCOUNT 模組：APPROVED      → DISBURSED
    public void handleStatusCallback(String applicationId, LoanStatusCallbackRequestDTO dto) {
        log.info("[StatusCallback] 收到回調 applicationId={}, caller={}, newStatus={}",
                applicationId, dto.getCallerModule(), dto.getNewStatus());
        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        String caller = dto.getCallerModule();

        if ("RISK".equals(caller)) {
            // 前置狀態：必須是 PENDING_REVIEW, RETURNED
            if (loan.getApplicationStatus() != LoanApplicationStatus.PENDING_REVIEW
                    && loan.getApplicationStatus() != LoanApplicationStatus.RETURNED) {
                throw new BusinessException(
                        "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用風控回調");
            }
            // 目標狀態允許 APPROVED / REJECTED / RETURNED
            if (dto.getNewStatus() != LoanApplicationStatus.APPROVED
                    && dto.getNewStatus() != LoanApplicationStatus.REJECTED
                    && dto.getNewStatus() != LoanApplicationStatus.RETURNED) {
                throw new BusinessException("風控回調目標狀態不合法：" + dto.getNewStatus());
            }

            // 1. 只要有傳入備註就更新 (不論狀態為何)
            if (dto.getAdminComment() != null && !dto.getAdminComment().isBlank()) {
                String comment = dto.getAdminComment();
                // 安全機制：若超過 50 字則截斷，避免資料庫報錯導致事務回滾
                if (comment.length() > 50) {
                    comment = comment.substring(0, 47) + "...";
                }
                loan.setReviewComment(comment);
            }

            // 攔截風控傳過來的「退回補件」通知
            if (dto.getNewStatus() == LoanApplicationStatus.RETURNED) {

                log.info("[RiskCallback] 收到風控退回補件通知。狀態改為 RETURNED，觸發客戶郵件通知。applicationId={}", applicationId);

                // 清空先前的送出時間，這樣前端網銀的「補件上傳按鈕」才會再度亮起允許客戶操作！
                loan.setApplicationStatus(LoanApplicationStatus.RETURNED);
                loan.setDocumentsSubmittedAt(null);
                loan.setCurrentSupplementBatchNo(safeBatchNo(loan.getCurrentSupplementBatchNo()) + 1);
                loan.setUpdateTime(LocalDateTime.now());

                List<String> docs = dto.getRequiredDocuments();
                if (docs != null && docs.size() == 1) {
                    String raw = docs.get(0).trim();
                    if (raw.startsWith("[")) {
                        // 對方傳了 JSON 字串，手動 parse
                        raw = raw.replaceAll("[\\[\\]\"]", "");
                        docs = List.of(raw.split(",\\s*"));
                    }
                }
                if (docs != null) {
                    docs = docs.stream()
                            .filter(doc -> doc != null && !doc.isBlank())
                            .map(String::trim)
                            .collect(Collectors.toList());
                }
                if (docs != null && !docs.isEmpty()) {
                    loan.setRequiredDocuments(String.join(",", docs));
                }

                String email = customerService.findEmailByCustomerId(loan.getCustomerId());
                log.info("[LoanCallback] 準備發送補件通知 email={}, applicationId={}", email, loan.getApplicationId());
                if (email != null) {
                    emailService.sendLoanDocumentRequiredNotification(
                            email,
                            loan.getApplicationId(),
                            loan.getApplyType(),
                            loan.getApplyAmount(),
                            docs,
                            dto.getAdminComment());
                    notificationService.createNotification(
                            loan.getCustomerId(),
                            NotificationType.LOAN,
                            "貸款需補件",
                            "您的貸款申請需要補件，請依通知準備文件。",
                            "/user/loan-status");
                } else {
                    log.warn("[LoanCallback] 客戶無 email，略過通知。customerId={}", loan.getCustomerId());
                }
                return; //處理完補件通知，直接結束方法
            }

            // 前置狀態：一般送審為 PENDING_REVIEW；退回補件後也允許風控改判核准/拒絕。
            if (loan.getApplicationStatus() != LoanApplicationStatus.PENDING_REVIEW
                    && loan.getApplicationStatus() != LoanApplicationStatus.RETURNED) {
                throw new BusinessException(
                        "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用風控回調");
            }
            // 目標狀態只允許 APPROVED / REJECTED
            if (dto.getNewStatus() != LoanApplicationStatus.APPROVED
                    && dto.getNewStatus() != LoanApplicationStatus.REJECTED) {
                throw new BusinessException("風控回調目標狀態不合法：" + dto.getNewStatus());
            }
            // 拒絕：主表寫入前先準備發信所需資料（寫入後 loan 狀態已變，需提前取值）
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
                            notificationService.createNotification(
                                    loan.getCustomerId(),
                                    NotificationType.LOAN,
                                    "貸款審核結果",
                                    "您的貸款申請未通過審核。",
                                    "/user/loan-status");
                        } else {
                            log.warn("[LoanRejected] 客戶無 email，略過通知。applicationId={}", rejectAppId);
                        }
                    }
                });
            }
            // 核准後觸發自動建帳與撥款（afterCommit 確保主表先寫入再執行）
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

            // 前置狀態：必須是 APPROVED
            if (loan.getApplicationStatus() != LoanApplicationStatus.APPROVED) {
                throw new BusinessException(
                        "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用帳戶撥款回調");
            }
            // 目標狀態只允許 DISBURSED
            if (dto.getNewStatus() != LoanApplicationStatus.DISBURSED) {
                throw new BusinessException("帳戶回調目標狀態不合法：" + dto.getNewStatus());
            }

        } else {
            throw new BusinessException("不認識的 callerModule：" + caller);
        }

        loan.setApplicationStatus(dto.getNewStatus());
        loan.setUpdateTime(LocalDateTime.now());
        laRepo.save(loan);
        // 強制寫入以確保後續邏輯或回傳能讀到最新值
        laRepo.flush();

        // ACCOUNT 模組撥款確認後，同步建立貸款帳戶
        if ("ACCOUNT".equals(caller)) {
            loanAccountService.createOnDisbursement(applicationId, dto.getLoanAccountNumber());

            // 核准暨撥款通知：帳號已建立，取 reviewDetail 取得核准條件
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
                    notificationService.createNotification(
                            loan.getCustomerId(),
                            NotificationType.LOAN,
                            "貸款已核准撥款",
                            "您的貸款已核准並完成撥款。",
                            "/user/loan-accounts");
                    sendLoanContractNotification(applicationId);
                } else {
                    log.warn("[LoanDisbursed] 略過通知：email={} detail={} applicationId={}",
                            disbEmail, detail, applicationId);
                }
            } catch (Exception e) {
                log.error("[LoanDisbursed] 發送核准暨撥款通知失敗，applicationId={}", applicationId, e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAccountDisbursedCallback(String applicationId) {
        handleAccountDisbursedCallback(applicationId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAccountDisbursedCallback(String applicationId, String loanAccountNumber) {
        LoanStatusCallbackRequestDTO callbackDto = new LoanStatusCallbackRequestDTO();
        callbackDto.setCallerModule("ACCOUNT");
        callbackDto.setNewStatus(LoanApplicationStatus.DISBURSED);
        callbackDto.setLoanAccountNumber(loanAccountNumber);
        callbackDto.setNote("account afterCommit: disbursement completed");
        handleStatusCallback(applicationId, callbackDto);
    }

    // 風控核准後自動建帳與撥款（由 handleStatusCallback APPROVED afterCommit 呼叫）
    // NOT_SUPPORTED：覆蓋 class 層級 @Transactional，使本方法不持有外層事務。
    // 如此 createLoanAccount 和 disburseLoan 各自以 REQUIRED 建立並提交自己的事務，
    // createLoanAccount 提交後 LOAN 帳戶已寫入 DB，disburseLoan 的 lockAccounts 才能查到它。
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

        // 步驟二：撥款（disburseLoan 的 afterCommit 會呼叫 handleStatusCallback ACCOUNT/DISBURSED）
        // lockAccounts 將同時鎖定：909000000001（銀行）、loanAccountNumber（LOAN帳）、disbursementAccount（客戶CHECKING）
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

    // 撥款補償：行員手動重送撥款（狀態必須仍是 APPROVED）
    // 對應 autoDisburse afterCommit 中失敗時保留 APPROVED 的補救路徑
    public void retryDisburse(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        if (loan.getApplicationStatus() != LoanApplicationStatus.APPROVED) {
            throw new BusinessException(
                    "此申請狀態為 " + loan.getApplicationStatus() + "，無需重送撥款（僅 APPROVED 可重送）");
        }

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

    // 風控送審補償：行員手動重送（狀態必須仍是 PENDING_REVIEW）
    // 對應 submitReview afterCommit 中 loanRiskClient 失敗的補救路徑
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

    private void sendLoanContractNotification(String applicationId) {
        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        String email = customerService.findEmailByCustomerId(loan.getCustomerId());
        if (email == null || email.isBlank()) {
            log.warn("[LoanContract] 客戶無 email，略過契約寄送。customerId={}", loan.getCustomerId());
            return;
        }

        byte[] pdfBytes = loanContractPdfService.generateContractPdf(applicationId);
        String customerName = customerProfileRepository.findById(loan.getCustomerId())
                .map(profile -> profile.getName())
                .orElse("親愛的客戶");
        String contractNo = formatContractNumber(loan.getApplicationId());

        emailService.sendLoanContractEmail(
                email,
                customerName,
                contractNo,
                "loan-contract-" + applicationId + ".pdf",
                pdfBytes);
        log.info("[LoanContract] 契約附件寄送完成 applicationId={} email={}", applicationId, email);
    }

    private String formatContractNumber(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return "-";
        }
        if (applicationId.startsWith("LA")) {
            return "LC-" + applicationId.substring(2);
        }
        return "LC-" + applicationId;
    }

    // 查填單內容
    public LoanReviewDetailResponseDTO getReviewDetail(String applicationId) {
        if (!laRepo.existsById(applicationId)) {
            throw new BusinessException("找不到申請編號：" + applicationId);
        }
        return reviewDetailRepo.findByApplicationId(applicationId)
                .map(this::toReviewDetailResponseDTO)
                .orElse(null);
    }

    // 置頂查詢：曾被外部模組（風控、帳戶）異動過狀態的申請，依更新時間降序
    public List<LoanApplicationResponseDTO> getRecentlyUpdated() {
        return laRepo.findByUpdateTimeIsNotNullOrderByUpdateTimeDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    // 回傳利率規則表，供前端頁面載入時抓取
    // 前端依此規則：
    // 1. 根據貸款種類顯示合法期數下拉選單
    // 2. 選完期數自動計算並顯示利率
    // 3. 送出時將算好的利率一併傳入後端

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
    // Entity → LoanApplicationResponseDTO
    private LoanApplicationResponseDTO toResponseDTO(LoanApplication loan) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 用 customerId 查出 cif 與姓名供前端顯示（Primary Key 查詢，效能无淣）
        customerProfileRepository.findById(loan.getCustomerId()).ifPresent(p -> {
            dto.setCif(p.getCif());
            dto.setMemberName(p.getName());
            dto.setPhone(p.getPhone());
            dto.setEmail(p.getEmail());
            dto.setCustomerPhone(p.getPhone());
            dto.setCustomerEmail(p.getEmail());
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
        // 帶入補件要求（風控退回時由 handleStatusCallback 寫入）
        if (loan.getRequiredDocuments() != null && !loan.getRequiredDocuments().isBlank()) {
            dto.setRequiredDocuments(List.of(loan.getRequiredDocuments().split(",")));
        }
        dto.setReviewComment(loan.getReviewComment());
        // 帶入二次填單確認值（有填單才有值，否則 null）
        reviewDetailRepo.findByApplicationId(loan.getApplicationId()).ifPresent(review -> {
            dto.setConfirmedAmount(review.getConfirmedAmount());
            dto.setConfirmedPeriod(review.getConfirmedPeriod());
            dto.setConfirmedRate(review.getConfirmedRate());
        });
        return dto;
    }

    private Integer safeBatchNo(Integer batchNo) {
        return batchNo == null ? 0 : batchNo;
    }

    // Entity → LoanContactLogResponseDTO
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

    // Entity → LoanReviewDetailResponseDTO
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
