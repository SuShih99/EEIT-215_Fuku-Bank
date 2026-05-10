package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.request.ExchangeRequest;
import com.javaeasybank.account.dto.request.ReversalRequest;
import com.javaeasybank.account.dto.request.TransferRequest;
import com.javaeasybank.account.dto.response.CashResponse;
import com.javaeasybank.account.dto.response.ExchangeResponse;
import com.javaeasybank.account.dto.response.ReversalResponse;
import com.javaeasybank.account.dto.response.TransferResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.exception.TransferException;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.account.utils.ReferenceIdGenerator;
import com.javaeasybank.common.service.ExchangeRateService;
import com.javaeasybank.risk.annotation.RiskCheck;
import com.javaeasybank.risk.core.enums.RiskScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final int TARGET_ACCOUNT_MIN_LENGTH = 6;
    private static final int TARGET_ACCOUNT_MAX_LENGTH = 20;
    private static final BigDecimal INTERBANK_FEE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal INTERBANK_FEE_LOW = new BigDecimal("10");
    private static final BigDecimal INTERBANK_FEE_HIGH = new BigDecimal("15");

    private final AccountRepository accountRepository;
    private final TransLogRepository transLogRepository;
    private final com.javaeasybank.customer.repository.CustomerProfileRepository customerProfileRepository;
    private final com.javaeasybank.common.service.EmailService emailService;
    private final ExchangeRateService exchangeRateService;

    /**
     * 執行國內轉帳。
     * 本行 909 轉帳會查詢目的帳戶並入帳；跨行轉帳只扣轉出帳戶，並額外寫入同業務編號的手續費紀錄。
     */
    @RiskCheck(scene = RiskScene.TRANSFER)//風控
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String fromAccNum = normalizeAccountNumber(request.getFromAccountNumber());
        String toAccNum = normalizeAccountNumber(request.getToAccountNumber());
        BigDecimal amount = request.getAmount();
        TransferBank toBank = TransferBank.fromCode(request.getToBankCode());
        boolean interbank = !toBank.isJavaBank();

        if (fromAccNum == null || toAccNum == null) {
            throw new TransferException("MISSING_ACCOUNT_NUMBER", "來源或目的帳號不可為空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("INVALID_AMOUNT", "轉帳金額必須大於 0");
        }
        validateTargetAccountNumber(toAccNum, interbank);

        if (!interbank && fromAccNum.equals(toAccNum)) {
            throw new TransferException("INVALID_TRANSFER", "來源與目的帳戶不可相同");
        }

        Account fromAccount = accountRepository.findById(fromAccNum)
                .orElseThrow(() -> new TransferException("SOURCE_ACCOUNT_NOT_FOUND", "來源帳戶不存在"));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("SOURCE_ACCOUNT_INACTIVE", "來源帳戶非 ACTIVE 狀態");
        }
        if (interbank && fromAccount.getCurrency() != Currency.TWD) {
            throw new TransferException("INTERBANK_TWD_ONLY", "跨行轉帳僅支援台幣帳戶");
        }

        BigDecimal feeAmount = interbank ? calculateInterbankFee(amount) : BigDecimal.ZERO;
        BigDecimal totalDebitAmount = amount.add(feeAmount);

        if (fromAccount.getBalance().compareTo(totalDebitAmount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "來源帳戶餘額不足");
        }

        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal toAccountBalance = null;

        if (interbank) {
            fromAccount.setBalance(fromBalanceBefore.subtract(totalDebitAmount));
            accountRepository.save(fromAccount);

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
                    "跨行轉帳手續費",
                    true,
                    feeAmount,
                    totalDebitAmount
            );

            transLogRepository.save(transferLog);
            transLogRepository.save(feeLog);
        } else {
            Account toAccount = accountRepository.findById(toAccNum)
                    .orElseThrow(() -> new TransferException("TARGET_ACCOUNT_NOT_FOUND", "目的帳戶不存在"));

            if (toAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new TransferException("TARGET_ACCOUNT_INACTIVE", "目的帳戶非 ACTIVE 狀態");
            }
            if (fromAccount.getCurrency() != toAccount.getCurrency()) {
                throw new TransferException("CURRENCY_MISMATCH", "來源與目的帳戶幣別不一致");
            }

            BigDecimal toBalanceBefore = toAccount.getBalance();
            fromAccount.setBalance(fromBalanceBefore.subtract(amount));
            toAccount.setBalance(toBalanceBefore.add(amount));

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

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

            transLogRepository.save(fromLog);
            transLogRepository.save(toLog);
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

        if (fromAccNum == null || toAccNum == null) {
            throw new TransferException("MISSING_ACCOUNT_NUMBER", "轉出或轉入帳戶不可為空");
        }
        if (fromAccNum.equals(toAccNum)) {
            throw new TransferException("INVALID_EXCHANGE_ACCOUNT", "轉出與轉入帳戶不可相同");
        }
        if (fromAmount == null || fromAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("INVALID_AMOUNT", "換匯金額必須大於 0");
        }

        Account fromAccount = accountRepository.findById(fromAccNum)
                .orElseThrow(() -> new TransferException("SOURCE_ACCOUNT_NOT_FOUND", "轉出帳戶不存在"));
        Account toAccount = accountRepository.findById(toAccNum)
                .orElseThrow(() -> new TransferException("TARGET_ACCOUNT_NOT_FOUND", "轉入帳戶不存在"));

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

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

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

        transLogRepository.save(fromLog);
        transLogRepository.save(toLog);

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
        if (account.getAccountType() == com.javaeasybank.account.enums.AccountType.LOAN) {
            throw new TransferException("INVALID_ACCOUNT_TYPE", label + "不可使用貸款帳戶");
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
        String accNum = request.getAccountNumber();
        BigDecimal amount = request.getAmount();

        // 1. 查帳戶
        Account account = accountRepository.findById(accNum)
                .orElseThrow(() -> new TransferException("ACCOUNT_NOT_FOUND", "帳戶不存在: " + accNum));

        // 2. 帳戶狀態檢查
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("ACCOUNT_INACTIVE", "帳戶非 ACTIVE 狀態");
        }

        // 3. 執行存款
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(balanceBefore.add(amount));
        accountRepository.save(account);

        // 4. 寫入交易紀錄（單筆，沒有對手方）
        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        TransLog transLog = new TransLog();
        transLog.setReferenceId(referenceId);
        transLog.setAccountNumber(accNum);
        transLog.setCounterpartAccount(null);       // 存款沒有對手方
        transLog.setEntryType(EntryType.CREDIT);    // 錢進來 = CREDIT
        transLog.setTransactionType(TransactionType.DEPOSIT);
        transLog.setAmount(amount);
        transLog.setBalanceBefore(balanceBefore);
        transLog.setBalanceAfter(account.getBalance());
        transLog.setCurrency(account.getCurrency());
        transLog.setNote(request.getNote());

        transLogRepository.save(transLog);

        log.info("存款成功: refId={}, account={}, amount={}", referenceId, accNum, amount);

        // 5. 組裝回傳
        CashResponse response = new CashResponse();
        response.setReferenceId(referenceId);
        response.setAccountNumber(accNum);
        response.setAmount(amount);
        response.setBalance(account.getBalance());
        response.setTransactedAt(now);
        return response;
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
        String accNum = request.getAccountNumber();
        BigDecimal amount = request.getAmount();

        // 1. 查帳戶
        Account account = accountRepository.findById(accNum)
                .orElseThrow(() -> new TransferException("ACCOUNT_NOT_FOUND", "帳戶不存在: " + accNum));

        // 2. 帳戶狀態檢查
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("ACCOUNT_INACTIVE", "帳戶非 ACTIVE 狀態");
        }

        // 3. 餘額充足性檢查
        if (account.getBalance().compareTo(amount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "帳戶餘額不足");
        }

        // 4. 執行提款
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(balanceBefore.subtract(amount));
        accountRepository.save(account);

        // 5. 寫入交易紀錄（單筆，沒有對手方）
        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        TransLog transLog = new TransLog();
        transLog.setReferenceId(referenceId);
        transLog.setAccountNumber(accNum);
        transLog.setCounterpartAccount(null);       // 提款沒有對手方
        transLog.setEntryType(EntryType.DEBIT);     // 錢出去 = DEBIT
        transLog.setTransactionType(TransactionType.WITHDRAW);
        transLog.setAmount(amount);
        transLog.setBalanceBefore(balanceBefore);
        transLog.setBalanceAfter(account.getBalance());
        transLog.setCurrency(account.getCurrency());
        transLog.setNote(request.getNote());

        transLogRepository.save(transLog);

        log.info("提款成功: refId={}, account={}, amount={}", referenceId, accNum, amount);

        // 6. 組裝回傳
        CashResponse response = new CashResponse();
        response.setReferenceId(referenceId);
        response.setAccountNumber(accNum);
        response.setAmount(amount);
        response.setBalance(account.getBalance());
        response.setTransactedAt(now);
        return response;
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
        String reversalNoteKeyword = "沖正 ref: " + originalRefId;
        boolean alreadyReversed = transLogRepository.existsByNoteContaining(reversalNoteKeyword);
        if (alreadyReversed) {
            throw new TransferException("ALREADY_REVERSED", "該交易已被沖正過: " + originalRefId);
        }

        // 3. 產生新的沖正 referenceId
        String reversalRefId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String notePrefix = "沖正 ref: " + originalRefId;
        if (request.getReason() != null && !request.getReason().isBlank()) {
            notePrefix += " | 原因: " + request.getReason();
        }

        List<ReversalResponse.ReversalDetail> details = new ArrayList<>();

        // 4. 對每筆原始紀錄做反向操作
        for (TransLog originalLog : originalLogs) {
            String accNum = originalLog.getAccountNumber();
            Account account = accountRepository.findById(accNum)
                    .orElseThrow(() -> new TransferException("ACCOUNT_NOT_FOUND", "帳戶不存在: " + accNum));

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

            accountRepository.save(account);

            // 5. 寫入沖正交易紀錄
            TransLog reversalLog = new TransLog();
            reversalLog.setReferenceId(reversalRefId);
            reversalLog.setAccountNumber(accNum);
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
            reversalLog.setBalanceAfter(account.getBalance());
            reversalLog.setCurrency(originalLog.getCurrency());
            reversalLog.setNote(notePrefix);

            transLogRepository.save(reversalLog);

            // 6. 收集沖正明細
            ReversalResponse.ReversalDetail detail = new ReversalResponse.ReversalDetail();
            detail.setAccountNumber(accNum);
            detail.setReversedAmount(originalLog.getAmount());
            detail.setBalanceAfter(account.getBalance());
            details.add(detail);
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
