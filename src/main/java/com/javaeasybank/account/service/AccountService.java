package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.AccountCreateRequest;
import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.exception.AccountException;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.utils.AccountDefaults;
import com.javaeasybank.account.utils.AccountNumberGenerator;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final List<AccountType> ADMIN_EXCLUDED_ACCOUNT_TYPES = List.of(AccountType.BUSINESS);

    private final AccountRepository accountRepository;
    private final CustomerProfileRepository customerProfileRepository;

    /**
     * 建立新帳戶。
     * 執行 KYC 驗證、帳戶類型規則驗證，並根據帳戶類型設定初始餘額和利率。
     *
     * @param request 帳戶創建請求。
     * @return 創建成功的帳戶響應。
     * @throws AccountException 如果 KYC 驗證失敗或帳戶類型規則不符。
     */
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        // 1. KYC 驗證 (Mock: 假設都通過)
        validateKyc(request.getCustomerId());

        // 2. 帳戶型別持有數規則驗證
        validateAccountTypeRules(request);

        // 3. 建立 Account Entity
        Account account = new Account();
        account.setAccountNumber(AccountNumberGenerator.generate());
        account.setCustomerId(request.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());
        account.setStatus(AccountStatus.PENDING); // 預設 PENDING
        
        // 處理子帳戶的父帳戶綁定
        if (request.getAccountType() == AccountType.SUB_ACCOUNT) {
             account.setParentAccountNumber(request.getParentAccountNumber());
        }

        // 4. 活存帳戶強制綁定初始餘額 1,000，並設定固定利率 0.15%
        if (request.getAccountType() == AccountType.CHECKING) {
            AccountDefaults.applyCheckingDefaults(account);
        } else if (request.getAccountType() == AccountType.SUB_ACCOUNT) {
             // 子帳戶利率比照活存
             AccountDefaults.applySubAccountDefaults(account);
        }

        // 儲存並回傳
        Account savedAccount = accountRepository.save(account);
        log.info("Successfully created account: {}", savedAccount.getAccountNumber());
        return toResponse(savedAccount);
    }

    /**
     * 根據帳號查詢單一帳戶。
     *
     * @param accountNumber 要查詢的帳號。
     * @return 查詢到的帳戶響應。
     * @throws AccountException 如果找不到指定帳號的帳戶。
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountException("ACCOUNT_NOT_FOUND", "Account not found: " + accountNumber));
        if (account.getAccountType() == AccountType.BUSINESS) {
            throw new AccountException("ACCOUNT_NOT_FOUND", "Account not found: " + accountNumber);
        }
        return toResponse(account);
    }

    /**
     * 根據客戶 ID 查詢所有帳戶並進行分頁。
     *
     * @param customerId 客戶 ID。
     * @param pageable   分頁資訊。
     * @return 包含帳戶響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByCustomerId(String customerId, Pageable pageable) {
        return accountRepository.findByCustomerIdAndAccountTypeNotIn(customerId, ADMIN_EXCLUDED_ACCOUNT_TYPES, pageable)
                .map(this::toResponse);
    }

    /**
     * 根據帳戶狀態篩選帳戶並進行分頁。
     *
     * @param status   要篩選的帳戶狀態。
     * @param pageable 分頁資訊。
     * @return 包含帳戶響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByStatus(AccountStatus status, Pageable pageable) {
        return accountRepository.findByStatusAndAccountTypeNot(status, AccountType.BUSINESS, pageable)
                .map(this::toResponse);
    }

    /**
     * 根據帳戶類型和貨幣篩選帳戶並進行分頁。
     *
     * @param type     要篩選的帳戶類型。
     * @param currency 要篩選的貨幣。
     * @param pageable 分頁資訊。
     * @return 包含帳戶響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByTypeAndCurrency(AccountType type, Currency currency, Pageable pageable) {
        if (type == AccountType.BUSINESS) {
            return Page.empty(pageable);
        }
        return accountRepository.findByAccountTypeAndCurrency(type, currency, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> searchAdminAccounts(
            String customerId,
            String customerName,
            String accountNumber,
            AccountStatus status,
            AccountType type,
            Currency currency,
            Pageable pageable) {
        if (type == AccountType.BUSINESS) {
            return Page.empty(pageable);
        }

        return accountRepository.searchAdminAccounts(
                        normalize(customerId),
                        normalize(customerName),
                        normalize(accountNumber),
                        status,
                        type,
                        currency,
                        ADMIN_EXCLUDED_ACCOUNT_TYPES,
                        pageable)
                .map(this::toResponse);
    }

    /**
     * 查詢所有帳戶並按建立時間倒序排序，並進行分頁。
     *
     * @param pageable 分頁資訊。
     * @return 包含帳戶響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> getLatest(Pageable pageable) {
        return accountRepository.findByAccountTypeNotOrderByCreatedAtDesc(AccountType.BUSINESS, pageable)
                .map(this::toResponse);
    }

    /**
     * 變更帳戶狀態。
     * 依據合法的狀態流轉規則進行驗證，不合法的轉換將被拒絕。
     *
     * 合法轉換:
     *   PENDING → ACTIVE
     *   ACTIVE → FROZEN, DORMANT, CLOSED
     *   FROZEN → ACTIVE, CLOSED
     *   DORMANT → ACTIVE, CLOSED
     *
     * @param accountNumber 帳號
     * @param newStatus 目標狀態
     * @return 更新後的帳戶響應
     */
    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountException("ACCOUNT_NOT_FOUND", "Account not found: " + accountNumber));
        if (account.getAccountType() == AccountType.BUSINESS) {
            throw new AccountException("ACCOUNT_NOT_FOUND", "Account not found: " + accountNumber);
        }

        AccountStatus currentStatus = account.getStatus();

        if (currentStatus == newStatus) {
            throw new AccountException("STATUS_UNCHANGED", "Account is already in " + newStatus + " status");
        }

        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new AccountException("INVALID_STATUS_TRANSITION",
                    "Cannot change status from " + currentStatus + " to " + newStatus);
        }

        // 凍結連動：如果目標狀態是 FROZEN，自動凍結同客戶下所有 ACTIVE 帳戶
        if (newStatus == AccountStatus.FROZEN) {
            List<Account> customerAccounts = accountRepository.findAllByCustomerId(account.getCustomerId());
            for (Account acc : customerAccounts) {
                if (!acc.getAccountNumber().equals(accountNumber) && acc.getStatus() == AccountStatus.ACTIVE) {
                    acc.setStatus(AccountStatus.FROZEN);
                    accountRepository.save(acc);
                    log.info("Account {} frozen (linked freeze from {})", acc.getAccountNumber(), accountNumber);
                }
            }
        }

        account.setStatus(newStatus);
        Account updated = accountRepository.save(account);
        log.info("Account {} status changed: {} → {}", accountNumber, currentStatus, newStatus);
        return toResponse(updated);
    }

    // ==========================================
    // Private Helper Methods
    // ==========================================

    /**
     * 驗證帳戶狀態轉換是否合法。
     * 依據開發文件定義的狀態機:
     *   PENDING → ACTIVE
     *   ACTIVE → FROZEN, DORMANT, CLOSED
     *   FROZEN → ACTIVE, CLOSED
     *   DORMANT → ACTIVE, CLOSED
     *   CLOSED → (不可轉換)
     *
     * @param from 目前狀態
     * @param to 目標狀態
     * @return 合法回傳 true，否則 false
     */
    private boolean isValidStatusTransition(AccountStatus from, AccountStatus to) {
        return switch (from) {
            case PENDING -> to == AccountStatus.ACTIVE;
            case ACTIVE -> to == AccountStatus.FROZEN || to == AccountStatus.DORMANT || to == AccountStatus.CLOSED;
            case FROZEN -> to == AccountStatus.ACTIVE || to == AccountStatus.CLOSED;
            case DORMANT -> to == AccountStatus.ACTIVE || to == AccountStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    /**
     * 將 Account Entity 轉為 AccountResponse，並查詢客戶姓名。
     */
    private AccountResponse toResponse(Account account) {
        String customerName = customerProfileRepository.findById(account.getCustomerId())
                .map(cp -> cp.getName())
                .orElse(null);
        return AccountResponse.fromEntity(account, customerName);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 執行客戶的 KYC 驗證。
     * 目前為模擬通過。
     *
     * @param customerId 客戶 ID。
     */
    private void validateKyc(String customerId) {
        if (!customerProfileRepository.existsById(customerId)) {
            throw new AccountException("CUSTOMER_NOT_FOUND", "客戶不存在：" + customerId);
        }
        log.debug("Customer validation passed for: {}", customerId);
    }

    /**
     * 驗證帳戶創建請求的帳戶類型規則。
     * 包括活存帳戶的唯一性、子帳戶的貨幣限制、父帳戶要求及所有權驗證。
     *
     * @param request 帳戶創建請求。
     * @throws AccountException 如果帳戶類型規則不符。
     */
    private void validateAccountTypeRules(AccountCreateRequest request) {
        String customerId = request.getCustomerId();
        AccountType type = request.getAccountType();
        Currency currency = request.getCurrency();

        if (type == AccountType.LOAN || type == AccountType.CREDIT_CARD || type == AccountType.BUSINESS) {
            throw new AccountException("DEDICATED_ACCOUNT_TYPE",
                    "此帳戶類型需透過專用業務服務建立：" + type);
        }

        if (type == AccountType.CHECKING) {
            // CHECKING(活存): 同客戶 + 同 currency 只能有一個
            boolean hasDuplicateChecking = accountRepository.existsByCustomerIdAndAccountTypeAndCurrency(customerId, type, currency);
            
            if (hasDuplicateChecking) {
                throw new AccountException("DUPLICATE_CHECKING_ACCOUNT", "Customer already has a checking account in " + currency);
            }
        } else if (type == AccountType.SUB_ACCOUNT) {
            // SUB_ACCOUNT(子帳戶): 限定台幣，且必須持有正常狀態(ACTIVE)的台幣活存
            if (currency != Currency.TWD) {
                throw new AccountException("SUB_ACCOUNT_CURRENCY_MUST_BE_TWD", "Sub-accounts must be in TWD");
            }

            boolean hasActiveTwdChecking = accountRepository.existsByCustomerIdAndAccountTypeAndCurrencyAndStatus(
                    customerId, AccountType.CHECKING, Currency.TWD, AccountStatus.ACTIVE);

            if (!hasActiveTwdChecking) {
                 throw new AccountException("NO_ACTIVE_TWD_CHECKING", "Customer must have an ACTIVE TWD checking account to create a sub-account");
            }

            if (request.getParentAccountNumber() == null || request.getParentAccountNumber().isBlank()) {
                throw new AccountException("PARENT_ACCOUNT_REQUIRED", "Sub-accounts must provide a parent account number");
            }

            // 子帳戶建立時，新增驗證: parentAccountNumber 對應的帳戶必須存在，且該帳戶的 customerId 必須與 request 的 customerId 一致。
            Account parentAccount = accountRepository.findById(request.getParentAccountNumber())
                    .orElseThrow(() -> new AccountException("PARENT_ACCOUNT_NOT_FOUND", "Parent account not found: " + request.getParentAccountNumber()));

            if (!parentAccount.getCustomerId().equals(customerId)) {
                throw new AccountException("PARENT_ACCOUNT_OWNER_MISMATCH", "Parent account does not belong to the same customer");
            }
        }
        // TIME_DEPOSIT(定存) 和 LOAN(貸款): 不檢查重複，可無限開
    }


}
