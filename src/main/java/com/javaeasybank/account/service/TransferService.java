package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.TransferRequest;
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
import com.javaeasybank.risk.annotation.RiskCheck;
import com.javaeasybank.risk.core.RiskScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @RiskCheck(scene = RiskScene.TRANSFER)//風控
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
}
