package com.javaeasybank.account.service;

import com.javaeasybank.account.TransferRiskClient;
import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.request.ExchangeRequest;
import com.javaeasybank.account.dto.request.ReversalRequest;
import com.javaeasybank.account.dto.request.TransferRequest;
import com.javaeasybank.account.dto.response.CashResponse;
import com.javaeasybank.account.dto.response.ExchangeResponse;
import com.javaeasybank.account.dto.response.ReversalResponse;
import com.javaeasybank.account.dto.response.TransferResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.PendingTransfer;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.exception.TransferException;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.PendingTransferRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.account.utils.ReferenceIdGenerator;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.common.service.ExchangeRateService;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.risk.dto.response.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.javaeasybank.risk.enums.Disposition.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final int TARGET_ACCOUNT_MIN_LENGTH = 6;
    private static final int TARGET_ACCOUNT_MAX_LENGTH = 20;
    private static final BigDecimal INTERBANK_FEE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal INTERBANK_FEE_LOW = new BigDecimal("10");
    private static final BigDecimal INTERBANK_FEE_HIGH = new BigDecimal("15");
    private static final String INTERBANK_FEE_NOTE = "跨行轉帳手續費";
    private static final String REVERSAL_NOTE_PREFIX = "沖正 ref: ";

    private final AccountRepository accountRepository;
    private final TransLogRepository transLogRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmailService emailService;
    private final ExchangeRateService exchangeRateService;
    private final TransferRiskClient transferRiskClient;
    private final PendingTransferRepository pendingTransferRepository;

    /**
     * 執行國內轉帳。
     * 本行 909 轉帳會查詢目的帳戶並入帳；跨行轉帳只扣轉出帳戶，並額外寫入同業務編號的手續費紀錄。
     */

    @Transactional
    public TransferResponse transfer(TransferRequest request) {

        String referenceId = ReferenceIdGenerator.generate();

        String fromAccNum = normalizeAccountNumber(request.getFromAccountNumber());
        String toAccNum = normalizeAccountNumber(request.getToAccountNumber());
        BigDecimal amount = request.getAmount();
        TransferBank toBank = TransferBank.fromCode(request.getToBankCode());
        boolean interbank = !toBank.isJavaBank();

        validateAccountNumbersPresent(fromAccNum, toAccNum, "來源或目的帳號不可為空");
        validatePositiveAmount(amount, "INVALID_AMOUNT", "轉帳金額必須大於 0");
        validateTargetAccountNumber(toAccNum, interbank);

        if (!interbank && fromAccNum.equals(toAccNum)) {
            throw new TransferException("INVALID_TRANSFER", "來源與目的帳戶不可相同");
        }

        Account fromAccount = findAccountOrThrow(fromAccNum, "SOURCE_ACCOUNT_NOT_FOUND", "來源帳戶不存在");

        validateActiveAccount(fromAccount, "SOURCE_ACCOUNT_INACTIVE", "來源帳戶非 ACTIVE 狀態");
        validateGeneralBalanceAccount(fromAccount, "來源帳戶");
        if (interbank && fromAccount.getCurrency() != Currency.TWD) {
            throw new TransferException("INTERBANK_TWD_ONLY", "跨行轉帳僅支援台幣帳戶");
        }

        // ── 黑名單比對 ──────────────────────────────────
        RiskCheckResponse riskResult = transferRiskClient.checkTransfer(
                fromAccount.getCustomerId(),
                toAccNum,
                amount,
                referenceId);

        switch (riskResult.getDisposition()) {
            case REJECT -> throw new TransferException(
                    "RISK_REJECTED", riskResult.getReason());
            case MANUAL_REVIEW -> {
                // 暫存交易，等人工審核
                savePendingTransfer(request, riskResult, referenceId);
                return TransferResponse.pending(
                        riskResult.getReason());
            }
            case PASS -> { /* 繼續正常轉帳流程 */ }
        }
        // ────────────────────────────────────────────────


        BigDecimal feeAmount = interbank ? calculateInterbankFee(amount) : BigDecimal.ZERO;
        BigDecimal totalDebitAmount = amount.add(feeAmount);

        if (fromAccount.getBalance().compareTo(totalDebitAmount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "來源帳戶餘額不足");
        }

        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal toAccountBalance = null;

        if (interbank) {
            fromAccount.setBalance(fromBalanceBefore.subtract(totalDebitAmount));
            saveAccounts(fromAccount);

            BigDecimal afterTransfer = fromBalanceBefore.subtract(amount);
            TransLog transferLog = buildTransLog(
                    referenceId,
                    fromAccNum,
                    toAccNum,
                    toBank,
                    EntryType.DEBIT,
                    TransactionType.TRANSFER,
                    amount,
                    fromBalanceBefore,
                    afterTransfer,
                    fromAccount.getCurrency(),
                    request.getNote(),
                    true,
                    feeAmount,
                    totalDebitAmount
            );

            TransLog feeLog = buildTransLog(
                    referenceId,
                    fromAccNum,
                    toAccNum,
                    toBank,
                    EntryType.DEBIT,
                    TransactionType.TRANSFER_FEE,
                    feeAmount,
                    afterTransfer,
                    fromAccount.getBalance(),
                    fromAccount.getCurrency(),
                    INTERBANK_FEE_NOTE,
                    true,
                    feeAmount,
                    totalDebitAmount
            );

            saveTransLogs(transferLog, feeLog);
        } else {
            Account toAccount = findAccountOrThrow(toAccNum, "TARGET_ACCOUNT_NOT_FOUND", "目的帳戶不存在");

            validateActiveAccount(toAccount, "TARGET_ACCOUNT_INACTIVE", "目的帳戶非 ACTIVE 狀態");
            validateGeneralBalanceAccount(toAccount, "目的帳戶");
            if (fromAccount.getCurrency() != toAccount.getCurrency()) {
                throw new TransferException("CURRENCY_MISMATCH", "來源與目的帳戶幣別不一致");
            }

            BigDecimal toBalanceBefore = toAccount.getBalance();
            fromAccount.setBalance(fromBalanceBefore.subtract(amount));
            toAccount.setBalance(toBalanceBefore.add(amount));

            saveAccounts(fromAccount, toAccount);

            TransLog fromLog = buildTransLog(
                    referenceId,
                    fromAccNum,
                    toAccNum,
                    TransferBank.JVB,
                    EntryType.DEBIT,
                    TransactionType.TRANSFER,
                    amount,
                    fromBalanceBefore,
                    fromAccount.getBalance(),
                    fromAccount.getCurrency(),
                    request.getNote(),
                    false,
                    BigDecimal.ZERO,
                    amount
            );

            TransLog toLog = buildTransLog(
                    referenceId,
                    toAccNum,
                    fromAccNum,
                    TransferBank.JVB,
                    EntryType.CREDIT,
                    TransactionType.TRANSFER,
                    amount,
                    toBalanceBefore,
                    toAccount.getBalance(),
                    toAccount.getCurrency(),
                    request.getNote(),
                    false,
                    BigDecimal.ZERO,
                    amount
            );

            saveTransLogs(fromLog, toLog);
            toAccountBalance = toAccount.getBalance();
        }

        log.info("轉帳成功: refId={}, from={}, toBank={}, to={}, amount={}, fee={}",
                referenceId, fromAccNum, toBank.getCode(), toAccNum, amount, feeAmount);

        customerProfileRepository.findById(fromAccount.getCustomerId())
                .ifPresent(profile -> {
                    if (profile.getEmail() != null) {
                        emailService.sendTransferNotification(
                                profile.getEmail(),
                                fromAccNum,
                                toAccNum,
                                amount,
                                fromAccount.getCurrency().name(),
                                referenceId
                        );
                    }
                });

        TransferResponse response = new TransferResponse();
        response.setReferenceId(referenceId);
        response.setFromAccountBalance(fromAccount.getBalance());
        response.setToAccountBalance(toAccountBalance);
        response.setAmount(amount);
        response.setFeeAmount(feeAmount);
        response.setTotalDebitAmount(totalDebitAmount);
        response.setInterbank(interbank);
        response.setToBankCode(toBank.getCode());
        response.setToBankName(toBank.getDisplayName());
        response.setTransferredAt(now);

        return response;
    }

    /**
     * 執行客戶本人帳戶間換匯。
     * 使用即時匯率 API 計算成交匯率，轉出帳戶扣原幣，轉入帳戶入目標幣，並寫入兩筆同 referenceId 的交易紀錄。
     */
    @Transactional
    public ExchangeResponse exchange(ExchangeRequest request, String customerId) {
        String fromAccNum = normalizeAccountNumber(request.getFromAccountNumber());
        String toAccNum = normalizeAccountNumber(request.getToAccountNumber());
        BigDecimal fromAmount = request.getFromAmount();

        validateAccountNumbersPresent(fromAccNum, toAccNum, "轉出或轉入帳戶不可為空");
        if (fromAccNum.equals(toAccNum)) {
            throw new TransferException("INVALID_EXCHANGE_ACCOUNT", "轉出與轉入帳戶不可相同");
        }
        validatePositiveAmount(fromAmount, "INVALID_AMOUNT", "換匯金額必須大於 0");

        Account fromAccount = findAccountOrThrow(fromAccNum, "SOURCE_ACCOUNT_NOT_FOUND", "轉出帳戶不存在");
        Account toAccount = findAccountOrThrow(toAccNum, "TARGET_ACCOUNT_NOT_FOUND", "轉入帳戶不存在");

        validateExchangeAccountOwner(fromAccount, customerId);
        validateExchangeAccountOwner(toAccount, customerId);
        validateExchangeAccountStatus(fromAccount, "轉出帳戶");
        validateExchangeAccountStatus(toAccount, "轉入帳戶");

        if (fromAccount.getCurrency() == toAccount.getCurrency()) {
            throw new TransferException("SAME_CURRENCY", "換匯需選擇不同幣別帳戶");
        }
        if (fromAccount.getCurrency() != Currency.TWD && toAccount.getCurrency() != Currency.TWD) {
            throw new TransferException("UNSUPPORTED_CROSS_CURRENCY_EXCHANGE", "換匯僅支援台幣換外幣或外幣換台幣");
        }

        BigDecimal normalizedFromAmount = fromAmount.setScale(fromAccount.getCurrency().getDecimalPlaces(), RoundingMode.HALF_UP);
        if (normalizedFromAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("INVALID_AMOUNT", "換匯金額必須大於 0");
        }
        if (fromAccount.getBalance().compareTo(normalizedFromAmount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "轉出帳戶餘額不足");
        }

        BigDecimal exchangeRate = exchangeRateService.calculateExchangeRate(fromAccount.getCurrency(), toAccount.getCurrency());
        BigDecimal toAmount = normalizedFromAmount
                .multiply(exchangeRate)
                .setScale(toAccount.getCurrency().getDecimalPlaces(), RoundingMode.HALF_UP);
        if (toAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("INVALID_EXCHANGE_AMOUNT", "換匯後金額不可為 0");
        }

        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        BigDecimal toBalanceBefore = toAccount.getBalance();
        fromAccount.setBalance(fromBalanceBefore.subtract(normalizedFromAmount));
        toAccount.setBalance(toBalanceBefore.add(toAmount));

        saveAccounts(fromAccount, toAccount);

        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String note = buildExchangeNote(request.getNote(), exchangeRate, fromAccount.getCurrency(), toAccount.getCurrency());

        TransLog fromLog = buildTransLog(
                referenceId,
                fromAccNum,
                toAccNum,
                TransferBank.JVB,
                EntryType.DEBIT,
                TransactionType.EXCHANGE,
                normalizedFromAmount,
                fromBalanceBefore,
                fromAccount.getBalance(),
                fromAccount.getCurrency(),
                note,
                false,
                BigDecimal.ZERO,
                normalizedFromAmount
        );

        TransLog toLog = buildTransLog(
                referenceId,
                toAccNum,
                fromAccNum,
                TransferBank.JVB,
                EntryType.CREDIT,
                TransactionType.EXCHANGE,
                toAmount,
                toBalanceBefore,
                toAccount.getBalance(),
                toAccount.getCurrency(),
                note,
                false,
                BigDecimal.ZERO,
                toAmount
        );

        saveTransLogs(fromLog, toLog);

        ExchangeResponse response = new ExchangeResponse();
        response.setReferenceId(referenceId);
        response.setFromAccountNumber(fromAccNum);
        response.setToAccountNumber(toAccNum);
        response.setFromCurrency(fromAccount.getCurrency());
        response.setToCurrency(toAccount.getCurrency());
        response.setFromAmount(normalizedFromAmount);
        response.setToAmount(toAmount);
        response.setExchangeRate(exchangeRate);
        response.setFromAccountBalance(fromAccount.getBalance());
        response.setToAccountBalance(toAccount.getBalance());
        response.setExchangedAt(now);
        return response;
    }

    private void validateExchangeAccountOwner(Account account, String customerId) {
        if (!account.getCustomerId().equals(customerId)) {
            throw new TransferException("ACCOUNT_NOT_OWNED", "帳戶不存在或不屬於您");
        }
    }

    private void validateExchangeAccountStatus(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("ACCOUNT_INACTIVE", label + "非 ACTIVE 狀態");
        }
        validateGeneralBalanceAccount(account, label);
    }

    private void validateGeneralBalanceAccount(Account account, String label) {
        if (!account.getAccountType().isGeneralBalanceAccount()) {
            throw new TransferException("INVALID_ACCOUNT_TYPE", label + "不可使用特殊帳戶類型：" + account.getAccountType());
        }
    }

    private String buildExchangeNote(String requestNote, BigDecimal exchangeRate, Currency fromCurrency, Currency toCurrency) {
        String note = "換匯 " + fromCurrency.name() + "->" + toCurrency.name() + " 匯率 " + exchangeRate.toPlainString();
        if (requestNote != null && !requestNote.isBlank()) {
            note += " | " + requestNote.trim();
        }
        return note;
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber == null ? null : accountNumber.trim();
    }

    private void validateTargetAccountNumber(String accountNumber, boolean interbank) {
        if (accountNumber.length() < TARGET_ACCOUNT_MIN_LENGTH || accountNumber.length() > TARGET_ACCOUNT_MAX_LENGTH) {
            throw new TransferException("INVALID_TARGET_ACCOUNT_LENGTH", "轉入帳號長度須為 6 到 20 碼");
        }
        if (!accountNumber.matches("\\d+")) {
            throw new TransferException("INVALID_TARGET_ACCOUNT_FORMAT", "轉入帳號僅可包含數字");
        }
        if (!interbank && accountNumber.length() != 12) {
            throw new TransferException("INVALID_LOCAL_TARGET_ACCOUNT_LENGTH", "本行轉帳目的帳號須為 12 碼");
        }
    }

    private BigDecimal calculateInterbankFee(BigDecimal amount) {
        return amount.compareTo(INTERBANK_FEE_THRESHOLD) <= 0 ? INTERBANK_FEE_LOW : INTERBANK_FEE_HIGH;
    }

    private void savePendingTransfer(
            TransferRequest request, RiskCheckResponse riskResult, String referenceId) {

        PendingTransfer pending = new PendingTransfer();
        pending.setReferenceId(referenceId);
        pending.setFromAccountNumber(
                normalizeAccountNumber(request.getFromAccountNumber()));
        pending.setToAccountNumber(
                normalizeAccountNumber(request.getToAccountNumber()));
        pending.setToBankCode(request.getToBankCode());
        pending.setAmount(request.getAmount());
        pending.setNote(request.getNote());
        pending.setRiskLogId(riskResult.getLogId());
        pending.setReviewTaskId(riskResult.getReviewTaskId());
        pending.setHoldReason(riskResult.getReason());
        pending.setStatus("PENDING");

        pendingTransferRepository.save(pending);
        log.info("[Transfer] 大額交易暫存 referenceId={} reviewTaskId={}",
                referenceId, riskResult.getReviewTaskId());
    }



    private TransLog buildTransLog(String referenceId,
                                  String accountNumber,
                                  String counterpartAccount,
                                  TransferBank counterpartBank,
                                  EntryType entryType,
                                  TransactionType transactionType,
                                  BigDecimal amount,
                                  BigDecimal balanceBefore,
                                  BigDecimal balanceAfter,
                                  Currency currency,
                                  String note,
                                  boolean interbank,
                                  BigDecimal feeAmount,
                                  BigDecimal totalDebitAmount) {
        TransLog transLog = new TransLog();
        transLog.setReferenceId(referenceId);
        transLog.setAccountNumber(accountNumber);
        transLog.setCounterpartAccount(counterpartAccount);
        transLog.setBankCode(TransferBank.JVB.getCode());
        transLog.setBankName(TransferBank.JVB.getDisplayName());
        transLog.setCounterpartBankCode(counterpartBank.getCode());
        transLog.setCounterpartBankName(counterpartBank.getDisplayName());
        transLog.setInterbank(interbank);
        transLog.setEntryType(entryType);
        transLog.setTransactionType(transactionType);
        transLog.setAmount(amount);
        transLog.setFeeAmount(feeAmount);
        transLog.setTotalDebitAmount(totalDebitAmount);
        transLog.setBalanceBefore(balanceBefore);
        transLog.setBalanceAfter(balanceAfter);
        transLog.setCurrency(currency);
        transLog.setNote(note);
        return transLog;
    }

    private void validateAccountNumbersPresent(String fromAccountNumber, String toAccountNumber, String message) {
        if (fromAccountNumber == null || toAccountNumber == null) {
            throw new TransferException("MISSING_ACCOUNT_NUMBER", message);
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String code, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException(code, message);
        }
    }

    private Account findAccountOrThrow(String accountNumber, String code, String message) {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new TransferException(code, message));
    }

    private void validateActiveAccount(Account account, String code, String message) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException(code, message);
        }
    }

    private void saveAccounts(Account... accounts) {
        for (Account account : accounts) {
            accountRepository.save(account);
        }
    }

    private void saveTransLogs(TransLog... transLogs) {
        for (TransLog transLog : transLogs) {
            transLogRepository.save(transLog);
        }
    }

    private CashResponse cashTransaction(CashRequest request,
                                         EntryType entryType,
                                         TransactionType transactionType,
                                         String actionLabel) {
        String accountNumber = normalizeAccountNumber(request.getAccountNumber());
        BigDecimal amount = request.getAmount();

        if (accountNumber == null) {
            throw new TransferException("MISSING_ACCOUNT_NUMBER", actionLabel + "帳號不可為空");
        }
        validatePositiveAmount(amount, "INVALID_AMOUNT", actionLabel + "金額必須大於 0");

        Account account = findAccountOrThrow(
                accountNumber,
                "ACCOUNT_NOT_FOUND",
                "帳戶不存在: " + accountNumber
        );
        validateActiveAccount(account, "ACCOUNT_INACTIVE", "帳戶非 ACTIVE 狀態");
        validateGeneralBalanceAccount(account, "帳戶");

        BigDecimal balanceBefore = account.getBalance();
        if (entryType == EntryType.DEBIT && balanceBefore.compareTo(amount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "帳戶餘額不足");
        }

        BigDecimal balanceAfter = entryType == EntryType.CREDIT
                ? balanceBefore.add(amount)
                : balanceBefore.subtract(amount);
        account.setBalance(balanceAfter);
        saveAccounts(account);

        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        saveTransLogs(buildCashTransLog(
                referenceId,
                account,
                entryType,
                transactionType,
                amount,
                balanceBefore,
                request.getNote()
        ));

        log.info("{}成功: refId={}, account={}, amount={}", actionLabel, referenceId, accountNumber, amount);
        return buildCashResponse(referenceId, accountNumber, amount, account.getBalance(), now);
    }

    private TransLog buildCashTransLog(String referenceId,
                                       Account account,
                                       EntryType entryType,
                                       TransactionType transactionType,
                                       BigDecimal amount,
                                       BigDecimal balanceBefore,
                                       String note) {
        TransLog transLog = new TransLog();
        transLog.setReferenceId(referenceId);
        transLog.setAccountNumber(account.getAccountNumber());
        transLog.setBankCode(TransferBank.JVB.getCode());
        transLog.setBankName(TransferBank.JVB.getDisplayName());
        transLog.setInterbank(false);
        transLog.setEntryType(entryType);
        transLog.setTransactionType(transactionType);
        transLog.setAmount(amount);
        transLog.setFeeAmount(BigDecimal.ZERO);
        transLog.setBalanceBefore(balanceBefore);
        transLog.setBalanceAfter(account.getBalance());
        transLog.setCurrency(account.getCurrency());
        transLog.setNote(note);
        return transLog;
    }

    private CashResponse buildCashResponse(String referenceId,
                                           String accountNumber,
                                           BigDecimal amount,
                                           BigDecimal balance,
                                           LocalDateTime transactedAt) {
        CashResponse response = new CashResponse();
        response.setReferenceId(referenceId);
        response.setAccountNumber(accountNumber);
        response.setAmount(amount);
        response.setBalance(balance);
        response.setTransactedAt(transactedAt);
        return response;
    }

    private TransLog buildReversalLog(String reversalRefId,
                                      TransLog originalLog,
                                      EntryType reversedEntryType,
                                      BigDecimal balanceBefore,
                                      BigDecimal balanceAfter,
                                      String note) {
        TransLog reversalLog = new TransLog();
        reversalLog.setReferenceId(reversalRefId);
        reversalLog.setAccountNumber(originalLog.getAccountNumber());
        reversalLog.setCounterpartAccount(originalLog.getCounterpartAccount());
        reversalLog.setBankCode(originalLog.getBankCode());
        reversalLog.setBankName(originalLog.getBankName());
        reversalLog.setCounterpartBankCode(originalLog.getCounterpartBankCode());
        reversalLog.setCounterpartBankName(originalLog.getCounterpartBankName());
        reversalLog.setInterbank(originalLog.isInterbank());
        reversalLog.setEntryType(reversedEntryType);
        reversalLog.setTransactionType(TransactionType.REVERSAL);
        reversalLog.setAmount(originalLog.getAmount());
        reversalLog.setFeeAmount(originalLog.getFeeAmount());
        reversalLog.setTotalDebitAmount(originalLog.getTotalDebitAmount());
        reversalLog.setBalanceBefore(balanceBefore);
        reversalLog.setBalanceAfter(balanceAfter);
        reversalLog.setCurrency(originalLog.getCurrency());
        reversalLog.setNote(note);
        return reversalLog;
    }

    private ReversalResponse.ReversalDetail buildReversalDetail(String accountNumber,
                                                                BigDecimal reversedAmount,
                                                                BigDecimal balanceAfter) {
        ReversalResponse.ReversalDetail detail = new ReversalResponse.ReversalDetail();
        detail.setAccountNumber(accountNumber);
        detail.setReversedAmount(reversedAmount);
        detail.setBalanceAfter(balanceAfter);
        return detail;
    }

    // ==========================================
    // 存款
    // ==========================================

    /**
     * 執行存款。
     * 驗證帳戶存在且為 ACTIVE 狀態，將金額加入帳戶餘額，並寫入一筆 CREDIT + DEPOSIT 交易紀錄。
     *
     * @param request 存款請求（帳號、金額、備註）。
     * @return 存款響應，包含交易編號、帳號、交易後餘額及交易時間。
     */
    @Transactional
    public CashResponse deposit(CashRequest request) {
        return cashTransaction(request, EntryType.CREDIT, TransactionType.DEPOSIT, "存款");
    }

    // ==========================================
    // 提款
    // ==========================================

    /**
     * 執行提款。
     * 驗證帳戶存在、ACTIVE 狀態且餘額充足，將金額從帳戶餘額扣除，並寫入一筆 DEBIT + WITHDRAW 交易紀錄。
     *
     * @param request 提款請求（帳號、金額、備註）。
     * @return 提款響應，包含交易編號、帳號、交易後餘額及交易時間。
     */
    @Transactional
    public CashResponse withdraw(CashRequest request) {
        return cashTransaction(request, EntryType.DEBIT, TransactionType.WITHDRAW, "提款");
    }

    // ==========================================
    // 沖正
    // ==========================================

    /**
     * 執行沖正（Reversal）。
     * 根據原始交易編號找出所有交易紀錄，對每筆紀錄反向操作帳戶餘額，
     * 並寫入新的沖正交易紀錄。不會修改或刪除原始紀錄。
     *
     * <p>沖正邏輯：
     * <ul>
     *   <li>原始 DEBIT（扣款）→ 沖正時把錢加回去，寫一筆 CREDIT + REVERSAL</li>
     *   <li>原始 CREDIT（入帳）→ 沖正時把錢扣回來，寫一筆 DEBIT + REVERSAL</li>
     * </ul>
     *
     * @param request 沖正請求（含原始交易編號與沖正原因）。
     * @return 沖正響應，包含新沖正編號與各帳戶沖正明細。
     * @throws TransferException 若原始交易不存在或已被沖正過。
     */
    @Transactional
    public ReversalResponse reversal(ReversalRequest request) {
        String originalRefId = request.getOriginalReferenceId();

        // 1. 查出原始交易紀錄
        List<TransLog> originalLogs = transLogRepository.findByReferenceId(originalRefId);
        if (originalLogs.isEmpty()) {
            throw new TransferException("TRANSACTION_NOT_FOUND", "找不到原始交易紀錄: " + originalRefId);
        }

        // 2. 防止重複沖正：檢查是否已有以此 referenceId 為目標的沖正紀錄
        //    沖正紀錄的 note 會包含 "沖正 ref: {originalRefId}"
        String reversalNoteKeyword = REVERSAL_NOTE_PREFIX + originalRefId;
        boolean alreadyReversed = transLogRepository.existsByNoteContaining(reversalNoteKeyword);
        if (alreadyReversed) {
            throw new TransferException("ALREADY_REVERSED", "該交易已被沖正過: " + originalRefId);
        }

        // 3. 產生新的沖正 referenceId
        String reversalRefId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String notePrefix = REVERSAL_NOTE_PREFIX + originalRefId;
        if (request.getReason() != null && !request.getReason().isBlank()) {
            notePrefix += " | 原因: " + request.getReason();
        }

        List<ReversalResponse.ReversalDetail> details = new ArrayList<>();

        // 4. 對每筆原始紀錄做反向操作
        for (TransLog originalLog : originalLogs) {
            String accNum = originalLog.getAccountNumber();
            Account account = findAccountOrThrow(accNum, "ACCOUNT_NOT_FOUND", "帳戶不存在: " + accNum);
            validateGeneralBalanceAccount(account, "沖正帳戶");

            BigDecimal balanceBefore = account.getBalance();
            EntryType reversedEntryType;

            if (originalLog.getEntryType() == EntryType.DEBIT) {
                // 原本扣款 → 沖正把錢加回來
                account.setBalance(balanceBefore.add(originalLog.getAmount()));
                reversedEntryType = EntryType.CREDIT;
            } else {
                // 原本入帳 → 沖正把錢扣回來
                if (account.getBalance().compareTo(originalLog.getAmount()) < 0) {
                    throw new TransferException("INSUFFICIENT_BALANCE",
                            "沖正失敗，帳戶 " + accNum + " 餘額不足以扣回");
                }
                account.setBalance(balanceBefore.subtract(originalLog.getAmount()));
                reversedEntryType = EntryType.DEBIT;
            }

            saveAccounts(account);

            saveTransLogs(buildReversalLog(
                    reversalRefId,
                    originalLog,
                    reversedEntryType,
                    balanceBefore,
                    account.getBalance(),
                    notePrefix
            ));

            // 6. 收集沖正明細
            details.add(buildReversalDetail(accNum, originalLog.getAmount(), account.getBalance()));
        }

        log.info("沖正成功: reversalRefId={}, originalRefId={}, 影響帳戶數={}", reversalRefId, originalRefId, details.size());

        // 7. 組裝回傳
        ReversalResponse response = new ReversalResponse();
        response.setReversalReferenceId(reversalRefId);
        response.setOriginalReferenceId(originalRefId);
        response.setDetails(details);
        response.setReversedAt(now);
        return response;
    }
}
