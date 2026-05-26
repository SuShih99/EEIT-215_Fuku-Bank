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
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
import com.javaeasybank.risk.enums.Occupation;
import com.javaeasybank.risk.service.CreditScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 開戶申請 Service — 處理申請提交、防堵檢查、風險標記、審核流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private static final int MAX_APPLICATIONS_PER_24H = 3;
    private static final String ACCOUNT_APPLICATION_TABLE = "ACCOUNT_APPLICATION";
    private final AccountApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;
    private final CreditScoreService creditScoreService;
    // 客戶端：提交申請

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
        if (!request.getAccountType().isCustomerVisible()) {
            throw new BusinessException("此帳戶類型需透過專用業務流程建立");
        }
        app.setAccountType(request.getAccountType());
        app.setCurrency(resolveCurrency(request));
        validateAccountApplicationRules(customerId, app.getAccountType(), app.getCurrency());

        // KYC
        app.setName(request.getCustomerName());
        app.setIdNumber(request.getIdNumber());
        app.setBirthday(request.getBirthday());
        app.setGender(request.getGender());
        app.setEmail(request.getEmail());
        app.setAddress(request.getAddress());
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
        syncCustomerProfileFromApplication(saved, request.getAnnualIncome());
        // 1. 字串安全轉為職業 Enum (模糊匹配)
        Occupation occupationEnum = Occupation.fromString(request.getOccupation());

        // 2. 將前端傳入的「萬元級距整數」還原為「真實金額 (元)」
        BigDecimal annualIncomeBigDecimal = BigDecimal.ZERO;
        if (request.getAnnualIncome() != null) {
            // 假設前端選 100，這裏轉成 100 * 10000 = 1,000,000 元
            annualIncomeBigDecimal = BigDecimal.valueOf(request.getAnnualIncome())
                    .multiply(BigDecimal.valueOf(10000));
        }
        creditScoreService.initializeCreditInfo(customerId,request.getBirthday(),occupationEnum,annualIncomeBigDecimal,request.getFundSource(),request.getIsPep());

        log.info("Account application submitted: id={}, customer={}, riskFlag={}",
                saved.getId(), customerId, riskFlag);

        return AccountApplicationResponse.fromEntity(saved);
    }
    // 客戶端：查詢我的申請

    @Transactional(readOnly = true)
    public List<AccountApplicationResponse> getMyApplications(String customerId) {
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(AccountApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }
    // 管理端：查詢申請列表

    @Transactional(readOnly = true)
    public Page<AccountApplicationResponse> listByStatus(ApplicationStatus status, Pageable pageable) {
        Set<String> columns = findAccountApplicationColumns();
        if (columns.isEmpty()) {
            log.warn("Cannot list account applications because table {} was not found", ACCOUNT_APPLICATION_TABLE);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Object> whereArgs = new ArrayList<>();
        String whereSql = "";
        if (status != null && columns.contains("status")) {
            whereSql = " WHERE [status] = ?";
            whereArgs.add(status.name());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + ACCOUNT_APPLICATION_TABLE + whereSql,
                Long.class,
                whereArgs.toArray());

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(pageable.getOffset());
        queryArgs.add(pageable.getPageSize());

        String orderColumn = columns.contains("created_at")
                ? "[created_at]"
                : columns.contains("id") ? "[id]" : "(SELECT NULL)";
        String sql = "SELECT " + buildAdminApplicationSelectClause(columns)
                + " FROM " + ACCOUNT_APPLICATION_TABLE
                + whereSql
                + " ORDER BY " + orderColumn + " DESC"
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<AccountApplicationResponse> content = jdbcTemplate.query(
                sql,
                this::mapAdminApplicationRow,
                queryArgs.toArray());

        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public AccountApplicationResponse getById(Long id) {
        AccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + id));
        return AccountApplicationResponse.fromEntityForAdmin(app);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("status", convertToMap(applicationRepository.countByStatus()));
        stats.put("riskFlag", convertToMap(applicationRepository.countByRiskFlag()));
        stats.put("accountType", convertToMap(applicationRepository.countByAccountType()));
        stats.put("totalApplications", applicationRepository.count());
        return stats;
    }

    private java.util.Map<String, Long> convertToMap(List<Object[]> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> ((Number) row[1]).longValue()
                ));
    }
    // 管理端：審核 — 通過

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
        if (!app.getAccountType().isCustomerVisible()) {
            throw new BusinessException("此帳戶類型需透過專用業務流程建立");
        }
        validateAccountApplicationRules(app.getCustomerId(), app.getAccountType(), app.getCurrency());

        // 建立帳戶
        Account account = new Account();
        account.setAccountNumber(AccountNumberGenerator.generate());
        account.setCustomerId(app.getCustomerId());
        account.setAccountType(app.getAccountType());
        account.setCurrency(app.getCurrency());
        account.setStatus(AccountStatus.ACTIVE);
        if (app.getAccountType() == AccountType.SUB_ACCOUNT) {
            account.setParentAccountNumber(resolveActiveTwdCheckingAccount(app.getCustomerId()).getAccountNumber());
        }

        // 活存與子帳戶設定初始餘額和利率
        if (app.getAccountType() == AccountType.CHECKING) {
            AccountDefaults.applyCheckingDefaults(account);
        } else if (app.getAccountType() == AccountType.SUB_ACCOUNT) {
            AccountDefaults.applySubAccountDefaults(account);
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
    // 管理端：審核 — 需補件

    @Transactional
    public AccountApplicationResponse requestSupplement(Long applicationId, String reason, String reviewedBy) {
        AccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請紀錄：" + applicationId));

        ApplicationStatus previousStatus = app.getStatus();
        if (previousStatus != ApplicationStatus.PENDING) {
            throw new BusinessException("此申請已非待審核狀態，無法要求補件");
        }

        app.setStatus(ApplicationStatus.SUPPLEMENT_REQUIRED);
        app.setRejectReason(reason);  // 複用此欄位記錄補件原因
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewedBy);

        AccountApplication updated = applicationRepository.save(app);
        syncCustomerProfileFromApplication(updated);
        notifySupplementRequired(updated, previousStatus);
        log.info("Account application supplement requested: id={}, reason={}", applicationId, reason);
        return AccountApplicationResponse.fromEntityForAdmin(updated);
    }
    // 管理端：審核 — 駁回

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
    // Private Helpers

    /**
     * 決定幣別：台幣活存 / 定存預設 TWD，外幣帳戶由前端指定。
     */
    private Currency resolveCurrency(AccountApplicationRequest request) {
        if (request.getAccountType() == AccountType.SUB_ACCOUNT) {
            return Currency.TWD;
        }
        if (request.getCurrency() != null) {
            return request.getCurrency();
        }
        // 預設 TWD
        return Currency.TWD;
    }

    private void validateAccountApplicationRules(String customerId, AccountType accountType, Currency currency) {
        if (accountType == AccountType.CHECKING) {
            if (accountRepository.existsByCustomerIdAndAccountTypeAndCurrency(customerId, accountType, currency)) {
                throw new BusinessException("該幣別活期存款帳戶已存在，無法重複申請");
            }
            return;
        }

        if (accountType == AccountType.SUB_ACCOUNT) {
            boolean hasActiveTwdChecking = accountRepository.existsByCustomerIdAndAccountTypeAndCurrencyAndStatus(
                    customerId,
                    AccountType.CHECKING,
                    Currency.TWD,
                    AccountStatus.ACTIVE);
            if (!hasActiveTwdChecking) {
                throw new BusinessException("需先擁有啟用中的台幣活期存款帳戶，才能申請子帳戶");
            }
        }
    }

    private Account resolveActiveTwdCheckingAccount(String customerId) {
        return accountRepository.findAllByCustomerIdAndAccountTypeAndCurrencyAndStatus(
                        customerId,
                        AccountType.CHECKING,
                        Currency.TWD,
                        AccountStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("需先擁有啟用中的台幣活期存款帳戶，才能申請子帳戶"));
    }

    private void syncCustomerProfileFromApplication(AccountApplication app) {
        syncCustomerProfileFromApplication(app, null);
    }

    private void syncCustomerProfileFromApplication(AccountApplication app, Integer annualIncome) {
        CustomerRespository.AccountApplicationProfileSyncRequest request =
                new CustomerRespository.AccountApplicationProfileSyncRequest();

        request.setName(app.getName());
        request.setIdNumber(app.getIdNumber());
        request.setBirthday(app.getBirthday());
        request.setGender(app.getGender());
        request.setEmail(app.getEmail());
        request.setAddress(app.getAddress());
        request.setNationality(app.getNationality());
        request.setPhone(app.getPhone());
        request.setRegisteredAddress(app.getRegisteredAddress());
        request.setCurrentAddress(app.getCurrentAddress());
        request.setOccupation(app.getOccupation());
        request.setEmployer(app.getEmployer());
        request.setAnnualIncome(annualIncome);
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

    private void notifySupplementRequired(AccountApplication app, ApplicationStatus previousStatus) {
        if (previousStatus == ApplicationStatus.SUPPLEMENT_REQUIRED) {
            return;
        }

        String reason = app.getRejectReason();
        String message = (reason != null && !reason.isBlank())
                ? "您的開戶申請需要補件：" + reason
                : "您的開戶申請需要補件，請查看申請紀錄。";

        notificationService.createNotification(
                app.getCustomerId(),
                NotificationType.ACCOUNT_SUPPLEMENT_REQUIRED,
                "開戶申請需補件",
                message,
                "/user/account-application");
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private Set<String> findAccountApplicationColumns() {
        List<String> columnNames = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                """, String.class, ACCOUNT_APPLICATION_TABLE);

        return columnNames.stream()
                .map(column -> column.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildAdminApplicationSelectClause(Set<String> columns) {
        List<String> selects = new ArrayList<>();
        selects.add(selectColumn(columns, "id", "id", "CAST(NULL AS BIGINT)"));
        selects.add(selectColumn(columns, "application_no", "application_no", applicationNoFallback(columns)));
        selects.add(selectColumn(columns, "customer_id", "customer_id", "N''"));
        selects.add(selectColumn(columns, "account_type", "account_type", "N'CHECKING'"));
        selects.add(selectColumn(columns, "currency", "currency", "N'TWD'"));
        selects.add(selectColumn(columns, "customer_name", "customer_name", "N''"));
        selects.add(selectColumn(columns, "id_number", "id_number", "N''"));
        selects.add(selectColumn(columns, "birthday", "birthday", "CAST(NULL AS DATE)"));
        selects.add(selectColumn(columns, "gender", "gender", "CAST(NULL AS NVARCHAR(1))"));
        selects.add(selectColumn(columns, "email", "email", "N''"));
        selects.add(selectColumn(columns, "address", "address", addressFallback(columns)));
        selects.add(selectColumn(columns, "nationality", "nationality", "N'TW'"));
        selects.add(selectColumn(columns, "phone", "phone", "N''"));
        selects.add(selectColumn(columns, "registered_address", "registered_address", "N''"));
        selects.add(selectColumn(columns, "current_address", "current_address", "N''"));
        selects.add(selectColumn(columns, "occupation", "occupation", "CAST(NULL AS NVARCHAR(50))"));
        selects.add(selectColumn(columns, "employer", "employer", "CAST(NULL AS NVARCHAR(100))"));
        selects.add(selectColumn(columns, "estimated_monthly_tx", "estimated_monthly_tx", "CAST(NULL AS INT)"));
        selects.add(selectColumn(columns, "account_purpose", "account_purpose", "CAST(NULL AS NVARCHAR(30))"));
        selects.add(selectColumn(columns, "fund_source", "fund_source", "CAST(NULL AS NVARCHAR(30))"));
        selects.add(selectColumn(columns, "tax_residency", "tax_residency", "N'TW'"));
        selects.add(selectColumn(columns, "is_pep", "is_pep", "CAST(0 AS BIT)"));
        selects.add(selectColumn(columns, "id_front_url", "id_front_url", "CAST(NULL AS NVARCHAR(255))"));
        selects.add(selectColumn(columns, "id_back_url", "id_back_url", "CAST(NULL AS NVARCHAR(255))"));
        selects.add(selectColumn(columns, "second_id_url", "second_id_url", "CAST(NULL AS NVARCHAR(255))"));
        selects.add(selectColumn(columns, "risk_flag", "risk_flag", "N'NORMAL'"));
        selects.add(selectColumn(columns, "status", "status", "N'PENDING'"));
        selects.add(selectColumn(columns, "reject_reason", "reject_reason", "CAST(NULL AS NVARCHAR(500))"));
        selects.add(selectColumn(columns, "reviewed_at", "reviewed_at", "CAST(NULL AS DATETIME2)"));
        selects.add(selectColumn(columns, "reviewed_by", "reviewed_by", "CAST(NULL AS NVARCHAR(50))"));
        selects.add(selectColumn(columns, "created_account_number", "created_account_number", "CAST(NULL AS NVARCHAR(14))"));
        selects.add(selectColumn(columns, "created_at", "created_at", "CAST(NULL AS DATETIME2)"));
        selects.add(selectColumn(columns, "updated_at", "updated_at", updatedAtFallback(columns)));
        return String.join(", ", selects);
    }

    private String selectColumn(Set<String> columns, String columnName, String alias, String fallbackExpression) {
        if (columns.contains(columnName.toLowerCase(Locale.ROOT))) {
            return "[" + columnName + "] AS [" + alias + "]";
        }
        return fallbackExpression + " AS [" + alias + "]";
    }

    private String applicationNoFallback(Set<String> columns) {
        return columns.contains("id") ? "CONCAT(N'APP-', [id])" : "N'-'";
    }

    private String addressFallback(Set<String> columns) {
        if (columns.contains("current_address") && columns.contains("registered_address")) {
            return "COALESCE([current_address], [registered_address], N'')";
        }
        if (columns.contains("current_address")) {
            return "COALESCE([current_address], N'')";
        }
        if (columns.contains("registered_address")) {
            return "COALESCE([registered_address], N'')";
        }
        return "N''";
    }

    private String updatedAtFallback(Set<String> columns) {
        return columns.contains("created_at") ? "[created_at]" : "CAST(NULL AS DATETIME2)";
    }

    private AccountApplicationResponse mapAdminApplicationRow(ResultSet rs, int rowNum) throws SQLException {
        return AccountApplicationResponse.builder()
                .id(nullableLong(rs, "id"))
                .applicationNo(rs.getString("application_no"))
                .customerId(rs.getString("customer_id"))
                .accountType(parseEnum(AccountType.class, rs.getString("account_type"), AccountType.CHECKING))
                .currency(parseEnum(Currency.class, rs.getString("currency"), Currency.TWD))
                .name(rs.getString("customer_name"))
                .idNumber(rs.getString("id_number"))
                .birthday(nullableDate(rs, "birthday"))
                .gender(rs.getString("gender"))
                .email(rs.getString("email"))
                .address(rs.getString("address"))
                .nationality(rs.getString("nationality"))
                .phone(rs.getString("phone"))
                .registeredAddress(rs.getString("registered_address"))
                .currentAddress(rs.getString("current_address"))
                .occupation(rs.getString("occupation"))
                .employer(rs.getString("employer"))
                .estimatedMonthlyTx(nullableInteger(rs, "estimated_monthly_tx"))
                .accountPurpose(parseEnum(AccountPurpose.class, rs.getString("account_purpose"), null))
                .fundSource(parseEnum(FundSource.class, rs.getString("fund_source"), null))
                .taxResidency(rs.getString("tax_residency"))
                .isPep(rs.getBoolean("is_pep"))
                .idFrontUrl(rs.getString("id_front_url"))
                .idBackUrl(rs.getString("id_back_url"))
                .secondIdUrl(rs.getString("second_id_url"))
                .riskFlag(parseEnum(RiskFlag.class, rs.getString("risk_flag"), RiskFlag.NORMAL))
                .status(parseEnum(ApplicationStatus.class, rs.getString("status"), ApplicationStatus.PENDING))
                .rejectReason(rs.getString("reject_reason"))
                .reviewedAt(nullableDateTime(rs, "reviewed_at"))
                .reviewedBy(rs.getString("reviewed_by"))
                .createdAccountNumber(rs.getString("created_account_number"))
                .createdAt(nullableDateTime(rs, "created_at"))
                .updatedAt(nullableDateTime(rs, "updated_at"))
                .build();
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private java.time.LocalDate nullableDate(ResultSet rs, String columnName) throws SQLException {
        Date value = rs.getDate(columnName);
        return value == null ? null : value.toLocalDate();
    }

    private java.time.LocalDateTime nullableDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported account application {} value: {}", enumType.getSimpleName(), rawValue);
            return fallback;
        }
    }
}
