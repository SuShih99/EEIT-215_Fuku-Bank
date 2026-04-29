package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.request.ReversalRequest;
import com.javaeasybank.account.dto.request.TransferRequest;
import com.javaeasybank.account.dto.response.CashResponse;
import com.javaeasybank.account.dto.response.ReversalResponse;
import com.javaeasybank.account.dto.response.TransferResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.exception.TransferException;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.account.utils.ReferenceIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransLogRepository transLogRepository;

    /**
     * 執行行內同幣別轉帳。
     * 包含基本驗證（金額、帳號非空、自我轉帳、帳戶狀態、幣別一致性、餘額充足性），
     * 執行扣款與入帳，並寫入交易紀錄。
     *
     * @param request 轉帳請求。
     * @return 轉帳響應，包含參考 ID、轉出轉入帳戶餘額及轉帳時間。
     * @throws TransferException 如果轉帳驗證失敗（例如帳戶不存在、餘額不足、幣別不符等）。
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String fromAccNum = request.getFromAccountNumber();
        String toAccNum = request.getToAccountNumber();
        BigDecimal amount = request.getAmount();

        if (fromAccNum == null || toAccNum == null) {
            throw new TransferException("MISSING_ACCOUNT_NUMBER", "來源或目的帳號不可為空");
        }

        // 1. 基本驗證
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("INVALID_AMOUNT", "轉帳金額必須大於 0");
        }

        // A4. 自我轉帳檢查
        if (fromAccNum.equals(toAccNum)) {
            throw new TransferException("INVALID_TRANSFER", "來源與目的帳戶不可相同");
        }

        Account fromAccount = accountRepository.findById(fromAccNum)
                .orElseThrow(() -> new TransferException("SOURCE_ACCOUNT_NOT_FOUND", "來源帳戶不存在"));

        Account toAccount = accountRepository.findById(toAccNum)
                .orElseThrow(() -> new TransferException("TARGET_ACCOUNT_NOT_FOUND", "目的帳戶不存在"));

        // A2. 帳戶狀態檢查
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("SOURCE_ACCOUNT_INACTIVE", "來源帳戶非 ACTIVE 狀態");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new TransferException("TARGET_ACCOUNT_INACTIVE", "目的帳戶非 ACTIVE 狀態");
        }

        // A5. 幣別一致性檢查
        if (fromAccount.getCurrency() != toAccount.getCurrency()) {
            throw new TransferException("CURRENCY_MISMATCH", "來源與目的帳戶幣別不一致");
        }

        // A3. 餘額充足性檢查
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new TransferException("INSUFFICIENT_BALANCE", "來源帳戶餘額不足");
        }

        // TODO: A1. 單筆大額交易檢查 (風險監控模組)
        // TODO: B1. 短時間高頻交易偵測 (交易紀錄模組 回查)
        // TODO: B2. 24 小時累積金額偵測 (交易紀錄模組 回查)

        // 2. 執行扣款與入帳 (同一 transaction 保證原子性)
        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        BigDecimal toBalanceBefore = toAccount.getBalance();

        fromAccount.setBalance(fromBalanceBefore.subtract(amount));
        toAccount.setBalance(toBalanceBefore.add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 3. 寫入交易紀錄 (雙邊記帳)
        String referenceId = ReferenceIdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        // 來源帳戶紀錄 (DEBIT 扣款)
        TransLog fromLog = new TransLog();
        fromLog.setReferenceId(referenceId);
        fromLog.setAccountNumber(fromAccNum);
        fromLog.setCounterpartAccount(toAccNum);
        fromLog.setEntryType(EntryType.DEBIT);
        fromLog.setTransactionType(TransactionType.TRANSFER);
        fromLog.setAmount(amount);
        fromLog.setBalanceBefore(fromBalanceBefore);
        fromLog.setBalanceAfter(fromAccount.getBalance());
        fromLog.setCurrency(fromAccount.getCurrency());
        fromLog.setNote(request.getNote());
        
        // 目的帳戶紀錄 (CREDIT 入帳)
        TransLog toLog = new TransLog();
        toLog.setReferenceId(referenceId);
        toLog.setAccountNumber(toAccNum);
        toLog.setCounterpartAccount(fromAccNum);
        toLog.setEntryType(EntryType.CREDIT);
        toLog.setTransactionType(TransactionType.TRANSFER);
        toLog.setAmount(amount);
        toLog.setBalanceBefore(toBalanceBefore);
        toLog.setBalanceAfter(toAccount.getBalance());
        toLog.setCurrency(toAccount.getCurrency());
        toLog.setNote(request.getNote());

        transLogRepository.save(fromLog);
        transLogRepository.save(toLog);

        log.info("轉帳成功: refId={}, from={}, to={}, amount={}", referenceId, fromAccNum, toAccNum, amount);

        // 4. 回傳結果
        TransferResponse response = new TransferResponse();
        response.setReferenceId(referenceId);
        response.setFromAccountBalance(fromAccount.getBalance());
        response.setToAccountBalance(toAccount.getBalance());
        response.setTransferredAt(now);

        return response;
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
            reversalLog.setEntryType(reversedEntryType);
            reversalLog.setTransactionType(TransactionType.REVERSAL);
            reversalLog.setAmount(originalLog.getAmount());
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
