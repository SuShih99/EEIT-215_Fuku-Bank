package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.ScheduledTransferRequest;
import com.javaeasybank.account.dto.response.ScheduledTransferResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.ScheduledTransfer;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.ScheduledTransferRepository;
import com.javaeasybank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<ScheduledTransferResponse> getByCustomerId(String customerId) {
        return scheduledTransferRepository.findByCustomerIdOrderByScheduledDateDesc(customerId)
                .stream()
                .map(ScheduledTransferResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ScheduledTransferResponse create(String customerId, ScheduledTransferRequest request) {
        TransferBank toBank = TransferBank.fromCode(request.getToBankCode());

        // 驗證轉出帳戶屬於該客戶
        Account fromAccount = accountRepository.findById(request.getFromAccountNumber())
                .orElseThrow(() -> new BusinessException("轉出帳戶不存在"));

        if (!fromAccount.getCustomerId().equals(customerId)) {
            throw new BusinessException("轉出帳戶不屬於您");
        }

        if (fromAccount.getAccountType() == com.javaeasybank.account.enums.AccountType.SUB_ACCOUNT) {
            throw new BusinessException("子帳戶無法轉出");
        }

        if (toBank.isJavaBank()) {
            if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
                throw new BusinessException("轉出與轉入帳戶不可相同");
            }
            Account toAccount = accountRepository.findById(request.getToAccountNumber())
                    .orElseThrow(() -> new BusinessException("轉入帳戶不存在"));
            if (toAccount.getAccountType() == com.javaeasybank.account.enums.AccountType.SUB_ACCOUNT) {
                if (!toAccount.getCustomerId().equals(customerId)) {
                    throw new BusinessException("只能轉入自己名下的子帳戶");
                }
            }
        }

        LocalDate scheduledDate = LocalDate.parse(request.getScheduledDate());
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new BusinessException("預約日期不可為過去");
        }

        ScheduledTransfer entity = new ScheduledTransfer();
        entity.setCustomerId(customerId);
        entity.setFromAccountNumber(request.getFromAccountNumber());
        entity.setToBankCode(toBank.getCode());
        entity.setToBankName(toBank.getDisplayName());
        entity.setToAccountNumber(request.getToAccountNumber());
        entity.setAmount(request.getAmount());
        entity.setScheduledDate(scheduledDate);
        entity.setNote(request.getNote());
        entity.setStatus("PENDING");

        ScheduledTransfer saved = scheduledTransferRepository.save(entity);
        log.info("Customer {} created scheduled transfer id: {} for date: {}", customerId, saved.getId(), scheduledDate);
        return ScheduledTransferResponse.fromEntity(saved);
    }

    @Transactional
    public void cancel(String customerId, Long id) {
        ScheduledTransfer entity = scheduledTransferRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new BusinessException("預約轉帳不存在"));

        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException("僅待執行的預約可取消");
        }

        entity.setStatus("CANCELLED");
        entity.setUpdatedAt(LocalDateTime.now());
        scheduledTransferRepository.save(entity);
        log.info("Customer {} cancelled scheduled transfer id: {}", customerId, id);
    }

    /**
     * 排程任務呼叫：執行到期的預約轉帳（由 @Scheduled 調用）
     */
    @Transactional
    public void executeDueTransfers() {
        List<ScheduledTransfer> dueList = scheduledTransferRepository
                .findByStatusAndScheduledDateLessThanEqual("PENDING", LocalDate.now());

        for (ScheduledTransfer st : dueList) {
            try {
                // 委託 TransferService 執行實際轉帳
                // 此處可注入 TransferService 來呼叫，暫以更新狀態代替
                st.setStatus("EXECUTED");
                st.setExecutedAt(LocalDateTime.now());
                scheduledTransferRepository.save(st);
                log.info("Executed scheduled transfer id: {}", st.getId());
            } catch (Exception e) {
                st.setStatus("FAILED");
                st.setFailReason(e.getMessage());
                scheduledTransferRepository.save(st);
                log.error("Failed to execute scheduled transfer id: {}", st.getId(), e);
            }
        }
    }
}
