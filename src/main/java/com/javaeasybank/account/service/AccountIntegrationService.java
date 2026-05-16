package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.CreditCardAccountCreateRequest;
import com.javaeasybank.account.dto.request.CreditCardPaymentRequest;
import com.javaeasybank.account.dto.request.CreditCardSettlementRequest;
import com.javaeasybank.account.dto.request.LoanAccountCreateRequest;
import com.javaeasybank.account.dto.request.LoanDisbursementRequest;
import com.javaeasybank.account.dto.request.LoanInterestRequest;
import com.javaeasybank.account.dto.request.LoanRepaymentRequest;
import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.response.CreditCardAccountResponse;
import com.javaeasybank.account.dto.response.CreditCardPaidAmountResponse;
import com.javaeasybank.account.dto.response.CreditCardPaymentResponse;
import com.javaeasybank.account.dto.response.CreditCardSettlementResponse;
import com.javaeasybank.account.dto.response.LoanAccountResponse;
import com.javaeasybank.account.dto.response.LoanAccountTransactionResponse;
import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.exception.AccountException;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.account.utils.ReferenceIdGenerator;
import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.BillStatus;
import com.javaeasybank.creditcard.repository.CardBillRepository;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.loan.dto.requests.LoanStatusCallbackRequestDTO;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.service.LoanApplicationService;
import com.javaeasybank.loan.service.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountIntegrationService {

    public static final String BANK_DISBURSEMENT_ACCOUNT_NUMBER = "909000000001";
    public static final String BANK_COLLECTION_ACCOUNT_NUMBER = "909000000002";
    private static final String SYSTEM_OPERATOR = "account-system";
    private static final int LOAN_ACCOUNT_PREFIX_START = 901;
    private static final int LOAN_ACCOUNT_PREFIX_END = 999;
    private static final int CREDIT_CARD_ACCOUNT_PREFIX_START = 801;
    private static final int CREDIT_CARD_ACCOUNT_PREFIX_END = 899;
    private static final int DEDICATED_ACCOUNT_NUMBER_LENGTH = 14;

    private final AccountRepository accountRepository;
    private final TransLogRepository transLogRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CardBillRepository cardBillRepository;
    private final CreditCardRepository creditCardRepository;

    // @Lazy 避免 account ↔ loan 模組啟動順序問題；無實際循環依賴
    @Lazy
    @Autowired
    private LoanApplicationService loanApplicationService;

    @Lazy
    @Autowired
    private LoanRepaymentService loanRepaymentService;

    @Transactional
    public LoanAccountResponse createLoanAccount(LoanAccountCreateRequest request) {
        CustomerProfile customer = findCustomer(request.getCustomerId());
        BigDecimal liability = normalizePositiveAmount(request.getLiability(), "貸款負債");
        BigDecimal rate = normalizeRate(request.getRate());
        String accountNumber = generateDedicatedAccountNumber(
                customer,
                LOAN_ACCOUNT_PREFIX_START,
                LOAN_ACCOUNT_PREFIX_END);

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomerId(customer.getCustomerId());
        account.setAccountType(AccountType.LOAN);
        account.setCurrency(Currency.TWD);
        account.setBalance(BigDecimal.ZERO);
        account.setLiability(liability);
        account.setInterestRate(rate);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedBy(SYSTEM_OPERATOR);
        account.setChangedBy(SYSTEM_OPERATOR);
        Account saved = accountRepository.save(account);

        log.info("[createLoanAccount] 貸款帳戶建立完成: customerId={}, loanAccountNumber={}",
                customer.getCustomerId(), accountNumber);
        return toLoanAccountResponse(saved);
    }

    @Transactional
    public LoanAccountTransactionResponse disburseLoan(LoanDisbursementRequest request) {
        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "撥款金額");
        String loanAccountNumber = normalizeAccountNumber(request.getLoanAccountNumber());
        String toAccountNumber = normalizeAccountNumber(request.getToAccountNumber());
        Map<String, Account> accounts = lockAccounts(
                BANK_DISBURSEMENT_ACCOUNT_NUMBER,
                loanAccountNumber,
                toAccountNumber);

        Account bankDisbursement = accounts.get(BANK_DISBURSEMENT_ACCOUNT_NUMBER);
        Account loanAccount = accounts.get(loanAccountNumber);
        Account targetAccount = accounts.get(toAccountNumber);

        validateBusinessAccount(bankDisbursement, BANK_DISBURSEMENT_ACCOUNT_NUMBER, "銀行撥款帳戶");
        validateLoanAccount(loanAccount);
        validateRepaymentSourceOrTarget(targetAccount, "撥款入帳帳戶");
        validateSameCustomer(loanAccount, targetAccount, "貸款帳戶與撥款入帳帳戶需屬於同一客戶");
        ensureLoanBalanceZero(loanAccount);
        ensureSufficientBalance(bankDisbursement, amount, "銀行撥款帳戶餘額不足");

        BigDecimal bankBefore = zeroIfNull(bankDisbursement.getBalance());
        BigDecimal targetBefore = zeroIfNull(targetAccount.getBalance());
        bankDisbursement.setBalance(bankBefore.subtract(amount));
        targetAccount.setBalance(targetBefore.add(amount));

        String referenceId = ReferenceIdGenerator.generate();
        String note = joinNote("貸款撥款 loanAccount=" + loanAccount.getAccountNumber(), request.getNote());
        transLogRepository.save(buildTransLog(
                referenceId,
                bankDisbursement,
                targetAccount.getAccountNumber(),
                EntryType.DEBIT,
                TransactionType.LOAN_DISBURSEMENT,
                amount,
                bankBefore,
                bankDisbursement.getBalance(),
                note));
        transLogRepository.save(buildTransLog(
                referenceId,
                targetAccount,
                bankDisbursement.getAccountNumber(),
                EntryType.CREDIT,
                TransactionType.LOAN_DISBURSEMENT,
                amount,
                targetBefore,
                targetAccount.getBalance(),
                note));

        // 撥款帳務完成後，通知 Loan 模組更新申請狀態並建立還款時間表
        // 使用 afterCommit 確保帳務事務先行提交，再開啟 Loan 側獨立事務
        if (request.getApplicationId() != null && !request.getApplicationId().isBlank()) {
            String appId = request.getApplicationId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("[Disbursement] 帳務事務提交，通知 Loan 模組 applicationId={}", appId);
                    try {
                        LoanStatusCallbackRequestDTO callbackDto = new LoanStatusCallbackRequestDTO();
                        callbackDto.setCallerModule("ACCOUNT");
                        callbackDto.setNewStatus(LoanApplicationStatus.DISBURSED);
                        loanApplicationService.handleStatusCallback(appId, callbackDto);
                    } catch (Exception e) {
                        log.error("[Disbursement] Loan 回調失敗 applicationId={}", appId, e);
                        // TODO（第五部）：寫入補傳表，搭配排程重試
                    }
                }
            });
        }

        return toLoanTransactionResponse(
                referenceId,
                loanAccount.getAccountNumber(),
                bankDisbursement.getAccountNumber(),
                targetAccount.getAccountNumber(),
                amount,
                loanAccount.getLiability(),
                bankDisbursement.getBalance(),
                targetAccount.getBalance());
    }

    @Transactional
    public LoanAccountTransactionResponse addLoanInterest(LoanInterestRequest request) {
        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "利息金額");
        Account loanAccount = lockSingleAccount(normalizeAccountNumber(request.getLoanAccountNumber()));
        validateLoanAccount(loanAccount);
        ensureLoanBalanceZero(loanAccount);

        BigDecimal liabilityBefore = zeroIfNull(loanAccount.getLiability());
        loanAccount.setLiability(liabilityBefore.add(amount));

        String referenceId = ReferenceIdGenerator.generate();
        transLogRepository.save(buildTransLog(
                referenceId,
                loanAccount,
                null,
                EntryType.DEBIT,
                TransactionType.INTEREST,
                amount,
                liabilityBefore,
                loanAccount.getLiability(),
                joinNote("貸款利息入帳", request.getNote())));

        return toLoanTransactionResponse(
                referenceId,
                loanAccount.getAccountNumber(),
                null,
                loanAccount.getAccountNumber(),
                amount,
                loanAccount.getLiability(),
                null,
                null);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getActiveTwdCheckingAccounts(String customerId) {
        findCustomer(customerId);
        return accountRepository.findAllByCustomerIdAndAccountTypeAndCurrencyAndStatus(
                customerId,
                AccountType.CHECKING,
                Currency.TWD,
                AccountStatus.ACTIVE)
                .stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    @Transactional
    public LoanAccountTransactionResponse repayLoan(String customerId, LoanRepaymentRequest request) {
        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "還款金額");
        String fromAccountNumber = normalizeAccountNumber(request.getFromAccountNumber());
        String loanAccountNumber = normalizeAccountNumber(request.getLoanAccountNumber());
        Map<String, Account> accounts = lockAccounts(
                fromAccountNumber,
                loanAccountNumber,
                BANK_COLLECTION_ACCOUNT_NUMBER);

        Account sourceAccount = accounts.get(fromAccountNumber);
        Account loanAccount = accounts.get(loanAccountNumber);
        Account bankCollection = accounts.get(BANK_COLLECTION_ACCOUNT_NUMBER);

        validateBusinessAccount(bankCollection, BANK_COLLECTION_ACCOUNT_NUMBER, "銀行收款帳戶");
        validateLoanAccount(loanAccount);
        validateRepaymentSourceOrTarget(sourceAccount, "扣款帳戶");
        validateCustomerOwns(sourceAccount, customerId, "扣款帳戶不存在或不屬於您");
        validateCustomerOwns(loanAccount, customerId, "貸款帳戶不存在或不屬於您");
        ensureLoanBalanceZero(loanAccount);
        ensureSufficientBalance(sourceAccount, amount, "扣款帳戶餘額不足");

        BigDecimal liabilityBefore = zeroIfNull(loanAccount.getLiability());
        if (amount.compareTo(liabilityBefore) > 0) {
            throw new AccountException("LOAN_OVERPAYMENT_NOT_ALLOWED", "還款金額不可大於剩餘負債");
        }

        BigDecimal sourceBefore = zeroIfNull(sourceAccount.getBalance());
        BigDecimal collectionBefore = zeroIfNull(bankCollection.getBalance());
        sourceAccount.setBalance(sourceBefore.subtract(amount));
        loanAccount.setLiability(liabilityBefore.subtract(amount));
        bankCollection.setBalance(collectionBefore.add(amount));

        String referenceId = ReferenceIdGenerator.generate();
        String note = joinNote("貸款還款 loanAccount=" + loanAccount.getAccountNumber(), request.getNote());
        transLogRepository.save(buildTransLog(referenceId, sourceAccount, loanAccount.getAccountNumber(),
                EntryType.DEBIT, TransactionType.LOAN_REPAYMENT, amount,
                sourceBefore, sourceAccount.getBalance(), note));
        transLogRepository.save(buildTransLog(referenceId, loanAccount, sourceAccount.getAccountNumber(),
                EntryType.CREDIT, TransactionType.LOAN_REPAYMENT, amount,
                liabilityBefore, loanAccount.getLiability(), note));
        transLogRepository.save(buildTransLog(referenceId, bankCollection, sourceAccount.getAccountNumber(),
                EntryType.CREDIT, TransactionType.LOAN_REPAYMENT, amount,
                collectionBefore, bankCollection.getBalance(), note));

        // 還款帳務完成後，通知 Loan 模組更新還款進度與帳戶狀態
        if (request.getApplicationId() != null && !request.getApplicationId().isBlank()) {
            String appId = request.getApplicationId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("[Repayment] 帳務事務提交，通知 Loan 模組 applicationId={}", appId);
                    try {
                        loanRepaymentService.processRepayment(appId);
                    } catch (Exception e) {
                        log.error("[Repayment] Loan 還款進度更新失敗 applicationId={} error={}",
                                appId, e.getMessage());
                        // TODO（第五部）：寫入補傳表，搭配排程重試
                    }
                }
            });
        }

        return toLoanTransactionResponse(
                referenceId,
                loanAccount.getAccountNumber(),
                sourceAccount.getAccountNumber(),
                bankCollection.getAccountNumber(),
                amount,
                loanAccount.getLiability(),
                sourceAccount.getBalance(),
                bankCollection.getBalance());
    }

    @Transactional(readOnly = true)
    public Page<TransLogResponse> getLoanRepaymentRecords(
            String customerId,
            String loanAccountNumber,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        List<String> loanAccountNumbers;
        if (loanAccountNumber != null && !loanAccountNumber.isBlank()) {
            Account loanAccount = accountRepository.findById(loanAccountNumber.trim())
                    .orElseThrow(() -> new AccountException("LOAN_ACCOUNT_NOT_FOUND", "貸款帳戶不存在"));
            validateLoanAccount(loanAccount);
            if (customerId != null) {
                validateCustomerOwns(loanAccount, customerId, "貸款帳戶不存在或不屬於您");
            }
            loanAccountNumbers = List.of(loanAccount.getAccountNumber());
        } else {
            if (customerId == null || customerId.isBlank()) {
                throw new AccountException("MISSING_QUERY_CONDITION", "請提供 customerId 或 loanAccountNumber");
            }
            loanAccountNumbers = accountRepository.findAllByCustomerIdAndAccountType(customerId, AccountType.LOAN)
                    .stream()
                    .map(Account::getAccountNumber)
                    .toList();
        }
        if (loanAccountNumbers.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<TransLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("accountNumber").in(loanAccountNumbers));
            predicates.add(cb.equal(root.get("transactionType"), TransactionType.LOAN_REPAYMENT));
            predicates.add(cb.equal(root.get("entryType"), EntryType.CREDIT));
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transLogRepository.findAll(spec, pageable).map(TransLogResponse::fromEntity);
    }

    @Transactional
    public CreditCardAccountResponse createCreditCardAccount(CreditCardAccountCreateRequest request) {
        CustomerProfile customer = findCustomer(request.getCustomerId());
        if (accountRepository.existsByCustomerIdAndAccountType(customer.getCustomerId(), AccountType.CREDIT_CARD)) {
            throw new AccountException("CREDIT_CARD_ACCOUNT_EXISTS", "此客戶已存在信用卡繳款帳戶");
        }

        String accountNumber = generateDedicatedAccountNumber(
                customer,
                CREDIT_CARD_ACCOUNT_PREFIX_START,
                CREDIT_CARD_ACCOUNT_PREFIX_END);
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomerId(customer.getCustomerId());
        account.setAccountType(AccountType.CREDIT_CARD);
        account.setCurrency(Currency.TWD);
        account.setBalance(BigDecimal.ZERO);
        account.setLiability(BigDecimal.ZERO);
        account.setInterestRate(null);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedBy(SYSTEM_OPERATOR);
        account.setChangedBy(SYSTEM_OPERATOR);

        Account saved = accountRepository.save(account);
        log.info("建立信用卡繳款帳戶成功: customerId={}, accountNumber={}", customer.getCustomerId(), accountNumber);
        return toCreditCardAccountResponse(saved);
    }

    @Transactional
    public CreditCardPaymentResponse payCreditCard(String customerId, CreditCardPaymentRequest request) {
        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "信用卡繳款金額");
        String fromAccountNumber = normalizeAccountNumber(request.getFromAccountNumber());
        String creditCardAccountNumber = normalizeAccountNumber(request.getCreditCardAccountNumber());
        Map<String, Account> accounts = lockAccounts(
                fromAccountNumber,
                creditCardAccountNumber);

        Account sourceAccount = accounts.get(fromAccountNumber);
        Account creditCardAccount = accounts.get(creditCardAccountNumber);

        validateRepaymentSourceOrTarget(sourceAccount, "扣款帳戶");
        validateCreditCardAccount(creditCardAccount);
        validateCustomerOwns(sourceAccount, customerId, "扣款帳戶不存在或不屬於您");
        validateCustomerOwns(creditCardAccount, customerId, "信用卡帳戶不存在或不屬於您");
        ensureSufficientBalance(sourceAccount, amount, "扣款帳戶餘額不足");

        BigDecimal sourceBefore = zeroIfNull(sourceAccount.getBalance());
        BigDecimal cardBefore = zeroIfNull(creditCardAccount.getBalance());
        sourceAccount.setBalance(sourceBefore.subtract(amount));
        creditCardAccount.setBalance(cardBefore.add(amount));

        // 取得最早未繳帳單，更新繳款金額與狀態
        CardBill bill = cardBillRepository
                .findTopByCardAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(customerId,
                        List.of(BillStatus.UNPAID, BillStatus.PARTIAL))
                .orElseThrow(() -> new AccountException("BILL_NOT_FOUND", "找不到未繳帳單"));
        // 更新帳單繳款金額與狀態
        bill.setPaidAmount(
                bill.getPaidAmount().add(amount));
        //回補信用卡可用餘額
        applyPaymentToCardDebts(bill, amount);

        // 已繳清
        if (bill.getPaidAmount().compareTo(bill.getTotalAmount()) >= 0) {

            bill.setBillStatus(BillStatus.PAID);

        }
        // 部分繳款
        else if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {

            bill.setBillStatus(BillStatus.PARTIAL);
        }

        cardBillRepository.save(bill);

        String referenceId = ReferenceIdGenerator.generate();
        String note = joinNote("信用卡繳款", request.getNote());
        transLogRepository.save(buildTransLog(referenceId, sourceAccount, creditCardAccount.getAccountNumber(),
                EntryType.DEBIT, TransactionType.CARD_PAYMENT, amount,
                sourceBefore, sourceAccount.getBalance(), note));
        transLogRepository.save(buildTransLog(referenceId, creditCardAccount, sourceAccount.getAccountNumber(),
                EntryType.CREDIT, TransactionType.CARD_PAYMENT, amount,
                cardBefore, creditCardAccount.getBalance(), note));

        CreditCardPaymentResponse response = new CreditCardPaymentResponse();
        response.setReferenceId(referenceId);
        response.setCreditCardAccountNumber(creditCardAccount.getAccountNumber());
        response.setFromAccountNumber(sourceAccount.getAccountNumber());
        response.setAmount(amount);
        response.setFromAccountBalance(sourceAccount.getBalance());
        response.setCreditCardBalance(creditCardAccount.getBalance());
        response.setTransactedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public CreditCardPaidAmountResponse getCreditCardPaidAmount(
            String customerId,
            String creditCardAccountNumber,
            String billingMonth,
            LocalDate startDate,
            LocalDate endDate) {

        String normalizedCreditCardAccountNumber = normalizeAccountNumber(creditCardAccountNumber);
        Account creditCardAccount = accountRepository.findById(normalizedCreditCardAccountNumber)
                .orElseThrow(() -> new AccountException("CREDIT_CARD_ACCOUNT_NOT_FOUND", "信用卡帳戶不存在"));
        validateCreditCardAccount(creditCardAccount);
        if (customerId != null && !customerId.isBlank()) {
            validateCustomerOwns(creditCardAccount, customerId, "信用卡帳戶不存在或不屬於您");
        }

        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endAt = endDate == null ? null : endDate.atTime(23, 59, 59);
        if (billingMonth != null && !billingMonth.isBlank()) {
            YearMonth month = YearMonth.parse(billingMonth);
            startAt = month.atDay(1).atStartOfDay();
            endAt = month.atEndOfMonth().atTime(23, 59, 59);
        }

        BigDecimal paidAmount = transLogRepository.sumAmountByAccountAndType(
                creditCardAccount.getAccountNumber(),
                TransactionType.CARD_PAYMENT,
                EntryType.CREDIT,
                startAt,
                endAt);

        CreditCardPaidAmountResponse response = new CreditCardPaidAmountResponse();
        response.setCreditCardAccountNumber(creditCardAccount.getAccountNumber());
        response.setPaidAmount(paidAmount);
        response.setCurrentCreditCardBalance(zeroIfNull(creditCardAccount.getBalance()));
        response.setBillingMonth(billingMonth);
        response.setStartDate(startDate == null ? null : startDate.toString());
        response.setEndDate(endDate == null ? null : endDate.toString());
        return response;
    }

    @Transactional
    public CreditCardSettlementResponse settleCreditCard(CreditCardSettlementRequest request) {
        BigDecimal amount = normalizePositiveAmount(request.getAmount(), "信用卡結算金額");
        String creditCardAccountNumber = normalizeAccountNumber(request.getCreditCardAccountNumber());
        Map<String, Account> accounts = lockAccounts(
                creditCardAccountNumber,
                BANK_COLLECTION_ACCOUNT_NUMBER);

        Account creditCardAccount = accounts.get(creditCardAccountNumber);
        Account bankCollection = accounts.get(BANK_COLLECTION_ACCOUNT_NUMBER);

        validateCreditCardAccount(creditCardAccount);
        validateBusinessAccount(bankCollection, BANK_COLLECTION_ACCOUNT_NUMBER, "銀行收款帳戶");
        ensureSufficientBalance(creditCardAccount, amount, "信用卡帳戶已繳金額不足");

        BigDecimal cardBefore = zeroIfNull(creditCardAccount.getBalance());
        BigDecimal collectionBefore = zeroIfNull(bankCollection.getBalance());
        creditCardAccount.setBalance(cardBefore.subtract(amount));
        bankCollection.setBalance(collectionBefore.add(amount));

        String referenceId = ReferenceIdGenerator.generate();
        String note = joinNote("信用卡帳務結算", request.getNote());
        transLogRepository.save(buildTransLog(referenceId, creditCardAccount, bankCollection.getAccountNumber(),
                EntryType.DEBIT, TransactionType.CARD_SETTLEMENT, amount,
                cardBefore, creditCardAccount.getBalance(), note));
        transLogRepository.save(buildTransLog(referenceId, bankCollection, creditCardAccount.getAccountNumber(),
                EntryType.CREDIT, TransactionType.CARD_SETTLEMENT, amount,
                collectionBefore, bankCollection.getBalance(), note));

        CreditCardSettlementResponse response = new CreditCardSettlementResponse();
        response.setReferenceId(referenceId);
        response.setCreditCardAccountNumber(creditCardAccount.getAccountNumber());
        response.setAmount(amount);
        response.setCreditCardBalance(creditCardAccount.getBalance());
        response.setBankCollectionBalance(bankCollection.getBalance());
        response.setTransactedAt(LocalDateTime.now());
        return response;
    }

    private CustomerProfile findCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new AccountException("MISSING_CUSTOMER_ID", "customerId 不可為空");
        }
        return customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new AccountException("CUSTOMER_NOT_FOUND", "客戶不存在：" + customerId));
    }

    private String generateDedicatedAccountNumber(CustomerProfile customer, int startPrefix, int endPrefix) {
        String identityPart = encodeIdentityNumber(customer.getIdNumber());
        for (int prefix = startPrefix; prefix <= endPrefix; prefix++) {
            String candidate = prefix + identityPart;
            if (candidate.length() > DEDICATED_ACCOUNT_NUMBER_LENGTH) {
                throw new AccountException("DEDICATED_ACCOUNT_NUMBER_TOO_LONG", "身分證編碼後帳號長度超過 14 碼");
            }
            if (!accountRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new AccountException("DEDICATED_ACCOUNT_NUMBER_EXHAUSTED", "可用帳號前綴已用完");
    }

    private String encodeIdentityNumber(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            throw new AccountException("MISSING_ID_NUMBER", "客戶身分證字號不可為空");
        }
        String normalized = idNumber.trim().toUpperCase();
        StringBuilder encoded = new StringBuilder();
        for (char ch : normalized.toCharArray()) {
            if (ch >= 'A' && ch <= 'Z') {
                encoded.append(String.format("%02d", ch - 'A' + 1));
            } else if (Character.isDigit(ch)) {
                encoded.append(ch);
            } else {
                throw new AccountException("INVALID_ID_NUMBER", "身分證字號只能包含英文字母與數字");
            }
        }
        return encoded.toString();
    }

    private Account lockSingleAccount(String accountNumber) {
        return lockAccounts(accountNumber).get(accountNumber);
    }

    private Map<String, Account> lockAccounts(String... accountNumbers) {
        List<String> normalizedNumbers = Arrays.stream(accountNumbers)
                .map(this::normalizeAccountNumber)
                .distinct()
                .sorted()
                .toList();
        List<Account> accounts = accountRepository.findAllByAccountNumberInForUpdate(normalizedNumbers);
        if (accounts.size() != normalizedNumbers.size()) {
            throw new AccountException("ACCOUNT_NOT_FOUND", "帳戶不存在或資料不完整");
        }
        return accounts.stream()
                .collect(Collectors.toMap(
                        Account::getAccountNumber,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new AccountException("MISSING_ACCOUNT_NUMBER", "帳號不可為空");
        }
        return accountNumber.trim();
    }

    private void validateBusinessAccount(Account account, String expectedAccountNumber, String label) {
        validateActive(account, label);
        if (account.getAccountType() != AccountType.BUSINESS
                || !expectedAccountNumber.equals(account.getAccountNumber())
                || account.getCurrency() != Currency.TWD
                || account.getInterestRate() != null) {
            throw new AccountException("INVALID_BUSINESS_ACCOUNT", label + "設定不正確");
        }
    }

    private void validateLoanAccount(Account account) {
        validateActive(account, "貸款帳戶");
        if (account.getAccountType() != AccountType.LOAN || account.getCurrency() != Currency.TWD) {
            throw new AccountException("INVALID_LOAN_ACCOUNT", "貸款帳戶設定不正確");
        }
    }

    private void validateCreditCardAccount(Account account) {
        validateActive(account, "信用卡帳戶");
        if (account.getAccountType() != AccountType.CREDIT_CARD
                || account.getCurrency() != Currency.TWD
                || account.getInterestRate() != null) {
            throw new AccountException("INVALID_CREDIT_CARD_ACCOUNT", "信用卡帳戶設定不正確");
        }
    }

    private void validateRepaymentSourceOrTarget(Account account, String label) {
        validateActive(account, label);
        if (account.getAccountType() != AccountType.CHECKING || account.getCurrency() != Currency.TWD) {
            throw new AccountException("INVALID_TWD_CHECKING_ACCOUNT", label + "必須為台幣活期存款帳戶");
        }
    }

    private void validateActive(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountException("ACCOUNT_INACTIVE", label + "非 ACTIVE 狀態");
        }
    }

    private void validateCustomerOwns(Account account, String customerId, String message) {
        if (!account.getCustomerId().equals(customerId)) {
            throw new AccountException("ACCOUNT_OWNER_MISMATCH", message);
        }
    }

    private void validateSameCustomer(Account first, Account second, String message) {
        if (!first.getCustomerId().equals(second.getCustomerId())) {
            throw new AccountException("ACCOUNT_OWNER_MISMATCH", message);
        }
    }

    private void ensureLoanBalanceZero(Account loanAccount) {
        if (loanAccount.getAccountType() == AccountType.LOAN
                && zeroIfNull(loanAccount.getBalance()).compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountException("INVALID_LOAN_BALANCE", "貸款帳戶不可有 balance 變化");
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal amount, String message) {
        if (zeroIfNull(account.getBalance()).compareTo(amount) < 0) {
            throw new AccountException("INSUFFICIENT_BALANCE", message);
        }
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountException("INVALID_AMOUNT", label + "必須大於 0");
        }
        return amount.setScale(Currency.TWD.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new AccountException("INVALID_RATE", "利率不可為空或小於 0");
        }
        return rate.setScale(5, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void applyPaymentToCardDebts(CardBill bill, BigDecimal amount) {
        if (bill == null || bill.getTransactions() == null || amount == null) {
            return;
        }

        BigDecimal remaining = amount;
        List<CreditCard> cards = bill.getTransactions().stream()
                .map(transaction -> transaction.getCard())
                .filter(card -> card != null)
                .distinct()
                .toList();

        for (CreditCard card : cards) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal currentDebt = zeroIfNull(card.getCurrentDebt());
            BigDecimal paymentApplied = currentDebt.min(remaining);
            card.setCurrentDebt(currentDebt.subtract(paymentApplied));
            remaining = remaining.subtract(paymentApplied);
        }

        creditCardRepository.saveAll(cards);
    }

    private TransLog buildTransLog(String referenceId,
            Account account,
            String counterpartAccount,
            EntryType entryType,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String note) {
        TransLog transLog = new TransLog();
        transLog.setReferenceId(referenceId);
        transLog.setAccountNumber(account.getAccountNumber());
        transLog.setCounterpartAccount(counterpartAccount);
        transLog.setBankCode(TransferBank.JVB.getCode());
        transLog.setBankName(TransferBank.JVB.getDisplayName());
        transLog.setCounterpartBankCode(TransferBank.JVB.getCode());
        transLog.setCounterpartBankName(TransferBank.JVB.getDisplayName());
        transLog.setInterbank(false);
        transLog.setEntryType(entryType);
        transLog.setTransactionType(transactionType);
        transLog.setAmount(amount);
        transLog.setFeeAmount(BigDecimal.ZERO);
        transLog.setTotalDebitAmount(amount);
        transLog.setBalanceBefore(balanceBefore);
        transLog.setBalanceAfter(balanceAfter);
        transLog.setCurrency(account.getCurrency());
        transLog.setNote(note);
        return transLog;
    }

    private String joinNote(String defaultNote, String note) {
        if (note == null || note.isBlank()) {
            return defaultNote;
        }
        return defaultNote + " | " + note.trim();
    }

    private LoanAccountResponse toLoanAccountResponse(Account account) {
        LoanAccountResponse response = new LoanAccountResponse();
        response.setLoanAccountNumber(account.getAccountNumber());
        response.setCustomerId(account.getCustomerId());
        response.setLiability(zeroIfNull(account.getLiability()));
        response.setRate(account.getInterestRate());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    private CreditCardAccountResponse toCreditCardAccountResponse(Account account) {
        CreditCardAccountResponse response = new CreditCardAccountResponse();
        response.setCreditCardAccountNumber(account.getAccountNumber());
        response.setCustomerId(account.getCustomerId());
        response.setBalance(zeroIfNull(account.getBalance()));
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    private LoanAccountTransactionResponse toLoanTransactionResponse(String referenceId,
            String loanAccountNumber,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            BigDecimal remainingLiability,
            BigDecimal sourceAccountBalance,
            BigDecimal targetAccountBalance) {
        LoanAccountTransactionResponse response = new LoanAccountTransactionResponse();
        response.setReferenceId(referenceId);
        response.setLoanAccountNumber(loanAccountNumber);
        response.setFromAccountNumber(fromAccountNumber);
        response.setToAccountNumber(toAccountNumber);
        response.setAmount(amount);
        response.setRemainingLiability(remainingLiability);
        response.setSourceAccountBalance(sourceAccountBalance);
        response.setTargetAccountBalance(targetAccountBalance);
        response.setTransactedAt(LocalDateTime.now());
        return response;
    }
}
