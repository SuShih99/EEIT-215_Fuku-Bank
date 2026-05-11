package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.AccountApplicationRequest;
import com.javaeasybank.account.dto.response.AccountApplicationResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.AccountApplication;
import com.javaeasybank.account.enums.*;
import com.javaeasybank.account.repository.AccountApplicationRepository;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.utils.AccountDefaults;
import com.javaeasybank.account.utils.AccountNumberGenerator;
import com.javaeasybank.account.utils.ApplicationNoGenerator;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 開戶申請 Service — 處理申請提交、防堵檢查、風險標記、審核流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private static final int MAX_APPLICATIONS_PER_24H = 3;
    private final AccountApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    // =========================================================
    // 客戶端：提交申請
    // =========================================================

    /**
     * 提交開戶申請。
     * 1. 檢查是否已有 PENDING 申請
     * 2. 同源防堵（IP + 手機 24h 內 ≤ 3 次）
     * 3. 風險標記（PEP / 高頻）
     * 4. 儲存申請
     */
    @Transactional
    public AccountApplicationResponse submit(String customerId,
                                             AccountApplicationRequest request,
                                             String idFrontUrl,
                                             String idBackUrl,
                                             String secondIdUrl,
                                             String applyIp) {

        // 1. 同客戶不可重複申請（已有 PENDING 狀態）
        if (applicationRepository.existsByCustomerIdAndStatus(customerId, ApplicationStatus.PENDING)) {
            throw new BusinessException("您已有一筆審核中的開戶申請，請等待審核完成");
        }

        // 2. 同源防堵：24h 內同 IP 或同手機號碼不可超過 3 次
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long ipCount = applicationRepository.countByApplyIpAndCreatedAtAfter(applyIp, since);
        long phoneCount = applicationRepository.countByPhoneAndCreatedAtAfter(request.getPhone(), since);

        boolean highFrequency = false;
        if (ipCount >= MAX_APPLICATIONS_PER_24H) {
            log.warn("Anti-fraud: IP {} has {} applications in 24h", applyIp, ipCount);
            highFrequency = true;
        }
        if (phoneCount >= MAX_APPLICATIONS_PER_24H) {
            log.warn("Anti-fraud: Phone {} has {} applications in 24h", request.getPhone(), phoneCount);
            highFrequency = true;
        }

        // 3. 風險標記
        boolean isPep = Boolean.TRUE.equals(request.getIsPep());
        RiskFlag riskFlag;
        if (isPep && highFrequency) {
            riskFlag = RiskFlag.PEP_HIGH_FREQUENCY;
        } else if (isPep) {
            riskFlag = RiskFlag.PEP;
        } else if (highFrequency) {
            riskFlag = RiskFlag.HIGH_FREQUENCY;
        } else {
            riskFlag = RiskFlag.NORMAL;
        }

        // 4. 組裝 Entity
        AccountApplication app = new AccountApplication();
        app.setApplicationNo(ApplicationNoGenerator.generate());
        app.setCustomerId(customerId);

        // 帳戶資訊
        app.setAccountType(request.getAccountType());
        app.setCurrency(resolveCurrency(request));

        // KYC
        app.setName(request.getCustomerName());
        app.setIdNumber(request.getIdNumber());
        app.setBirthday(request.getBirthday());
        app.setNationality(request.getNationality());
        app.setPhone(request.getPhone());
        app.setRegisteredAddress(request.getRegisteredAddress());
        app.setCurrentAddress(request.getCurrentAddress());

        // 職業
        app.setOccupation(request.getOccupation());
        app.setEmployer(request.getEmployer());
        app.setEstimatedMonthlyTx(request.getEstimatedMonthlyTx());

        // 目的 & 資金來源
        app.setAccountPurpose(request.getAccountPurpose());
        app.setFundSource(request.getFundSource());

        // 法遵
        app.setTaxResidency(request.getTaxResidency());
        app.setIsPep(isPep);

        // 證件圖片
        app.setIdFrontUrl(idFrontUrl);
        app.setIdBackUrl(idBackUrl);
        app.setSecondIdUrl(secondIdUrl);

        // 風控
        app.setRiskFlag(riskFlag);
        app.setApplyIp(applyIp);

        // 審核狀態
        app.setStatus(ApplicationStatus.PENDING);

        AccountApplication saved = applicationRepository.save(app);
        syncCustomerProfileFromApplication(saved);
        log.info("Account application submitted: id={}, customer={}, riskFlag={}",
                saved.getId(), customerId, riskFlag);

        return AccountApplicationResponse.fromEntity(saved);
    }

    // =========================================================
    // 客戶端：查詢我的申請
    // =========================================================

    @Transactional(readOnly = true)
    public List<AccountApplicationResponse> getMyApplications(String customerId) {
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(AccountApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // =========================================================
    // 管理端：查詢申請列表
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AccountApplicationResponse> listByStatus(ApplicationStatus status, Pageable pageable) {
        Page<AccountApplication> page;
        if (status != null) {
            page = applicationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = applicationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(AccountApplicationResponse::fromEntityForAdmin);
    }

    @Transactional(readOnly = true)
    public AccountApplicationResponse getById(Long id) {
        AccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + id));
        return AccountApplicationResponse.fromEntityForAdmin(app);
    }

    // =========================================================
    // 管理端：審核 — 通過
    // =========================================================

    /**
     * 審核通過：自動建立 Account，並回寫帳號至申請單。
     */
    @Transactional
    public AccountApplicationResponse approve(Long applicationId, String reviewedBy) {
        AccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + applicationId));

        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException("此申請已非待審核狀態，無法核准");
        }

        // 建立帳戶
        Account account = new Account();
        account.setAccountNumber(AccountNumberGenerator.generate());
        account.setCustomerId(app.getCustomerId());
        account.setAccountType(app.getAccountType());
        account.setCurrency(app.getCurrency());
        account.setStatus(AccountStatus.ACTIVE);

        // 活存帳戶設定初始餘額和利率
        if (app.getAccountType() == AccountType.CHECKING) {
            AccountDefaults.applyCheckingDefaults(account);
        }

        Account savedAccount = accountRepository.save(account);
        log.info("Account created from application: accountNumber={}, applicationId={}",
                savedAccount.getAccountNumber(), applicationId);

        // 更新申請狀態
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewedBy);
        app.setCreatedAccountNumber(savedAccount.getAccountNumber());

        AccountApplication updated = applicationRepository.save(app);
        syncCustomerProfileFromApplication(updated);
        return AccountApplicationResponse.fromEntityForAdmin(updated);
    }

    // =========================================================
    // 管理端：審核 — 需補件
    // =========================================================

    @Transactional
    public AccountApplicationResponse requestSupplement(Long applicationId, String reason, String reviewedBy) {
        AccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + applicationId));

        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException("此申請已非待審核狀態，無法要求補件");
        }

        app.setStatus(ApplicationStatus.SUPPLEMENT_REQUIRED);
        app.setRejectReason(reason);  // 複用此欄位記錄補件原因
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewedBy);

        AccountApplication updated = applicationRepository.save(app);
        syncCustomerProfileFromApplication(updated);
        log.info("Account application supplement requested: id={}, reason={}", applicationId, reason);
        return AccountApplicationResponse.fromEntityForAdmin(updated);
    }

    // =========================================================
    // 管理端：審核 — 駁回
    // =========================================================

    @Transactional
    public AccountApplicationResponse reject(Long applicationId, String rejectReason, String reviewedBy) {
        AccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + applicationId));

        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException("此申請已非待審核狀態，無法駁回");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectReason(rejectReason);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewedBy);

        AccountApplication updated = applicationRepository.save(app);
        syncCustomerProfileFromApplication(updated);
        log.info("Account application rejected: id={}, reason={}", applicationId, rejectReason);
        return AccountApplicationResponse.fromEntityForAdmin(updated);
    }

    // =========================================================
    // Private Helpers
    // =========================================================

    /**
     * 決定幣別：台幣活存 / 定存預設 TWD，外幣帳戶由前端指定。
     */
    private Currency resolveCurrency(AccountApplicationRequest request) {
        if (request.getCurrency() != null) {
            return request.getCurrency();
        }
        // 預設 TWD
        return Currency.TWD;
    }

    private void syncCustomerProfileFromApplication(AccountApplication app) {
        CustomerRespository.AccountApplicationProfileSyncRequest request =
                new CustomerRespository.AccountApplicationProfileSyncRequest();

        request.setName(app.getName());
        request.setIdNumber(app.getIdNumber());
        request.setBirthday(app.getBirthday());
        request.setNationality(app.getNationality());
        request.setPhone(app.getPhone());
        request.setRegisteredAddress(app.getRegisteredAddress());
        request.setCurrentAddress(app.getCurrentAddress());
        request.setOccupation(app.getOccupation());
        request.setEmployer(app.getEmployer());
        request.setEstimatedMonthlyTx(app.getEstimatedMonthlyTx());
        request.setAccountPurpose(enumName(app.getAccountPurpose()));
        request.setFundSource(enumName(app.getFundSource()));
        request.setTaxResidency(app.getTaxResidency());
        request.setIsPep(app.getIsPep());
        request.setIdFrontUrl(app.getIdFrontUrl());
        request.setIdBackUrl(app.getIdBackUrl());
        request.setSecondIdUrl(app.getSecondIdUrl());
        request.setLatestAccountApplicationId(app.getId());
        request.setLatestAccountApplicationNo(app.getApplicationNo());
        request.setLatestAppliedAccountType(enumName(app.getAccountType()));
        request.setLatestAppliedCurrency(enumName(app.getCurrency()));
        request.setLatestAccountApplicationStatus(enumName(app.getStatus()));
        request.setLatestAccountApplicationRiskFlag(enumName(app.getRiskFlag()));
        request.setLatestAccountApplicationReviewedAt(app.getReviewedAt());
        request.setLatestAccountApplicationReviewedBy(app.getReviewedBy());
        request.setLatestAccountApplicationRejectReason(app.getRejectReason());
        request.setCreatedAccountNumber(app.getCreatedAccountNumber());

        customerService.syncAccountApplicationProfile(app.getCustomerId(), request);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
