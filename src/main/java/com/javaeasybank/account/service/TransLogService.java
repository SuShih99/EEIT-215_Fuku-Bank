package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 交易紀錄業務邏輯服務層。
 * 處理交易紀錄的查詢操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransLogService {

    private final TransLogRepository transLogRepository;
    private final AccountRepository accountRepository;

    /**
     * 根據參考 ID 查詢交易紀錄列表。
     *
     * @param referenceId 業務交易參考 ID。
     * @return 包含交易紀錄響應的列表。
     */
    @Transactional(readOnly = true)
    public List<TransLogResponse> getByReferenceId(String referenceId) {
        log.debug("Fetching trans logs by referenceId: {}", referenceId);
        return toAdminResponses(transLogRepository.findByReferenceIdExcludingAccountType(referenceId, AccountType.BUSINESS));
    }

    /**
     * 根據帳號查詢相關的交易紀錄分頁列表。
     *
     * @param accountNumber 帳號。
     * @param pageable      分頁資訊。
     * @return 包含交易紀錄響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<TransLogResponse> getByAccountNumber(String accountNumber, Pageable pageable) {
        log.debug("Fetching trans logs by accountNumber: {}", accountNumber);
        if (accountRepository.existsByAccountNumberAndAccountType(accountNumber, AccountType.BUSINESS)) {
            return Page.empty(pageable);
        }
        return toAdminResponsePage(
                transLogRepository.findByAccountInvolvedExcludingAccountType(accountNumber, AccountType.BUSINESS, pageable));
    }

    /**
     * 根據客戶 ID 查詢該客戶名下所有帳戶的交易紀錄分頁列表。
     *
     * @param customerId 客戶 ID。
     * @param pageable   分頁資訊。
     * @return 包含交易紀錄響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<TransLogResponse> getByCustomerId(String customerId, Pageable pageable) {
        log.debug("Fetching trans logs by customerId: {}", customerId);
        return toAdminResponsePage(
                transLogRepository.findByCustomerIdExcludingAccountType(customerId, AccountType.BUSINESS, pageable));
    }


    @Transactional(readOnly = true)
    public Page<TransLogResponse> getByCustomerIdAndDateRange(String customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching trans logs by customerId: {} and date range: {} to {}", customerId, startDate, endDate);
        return toAdminResponsePage(
                transLogRepository.findByCustomerIdAndDateRangeExcludingAccountType(
                        customerId, AccountType.BUSINESS, startDate, endDate, pageable));
    }

    /**
     * 查詢所有交易紀錄並按建立時間倒序排序，並進行分頁。
     *
     * @param pageable 分頁資訊。
     * @return 包含交易紀錄響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<TransLogResponse> getLatest(Pageable pageable) {
        log.debug("Fetching latest trans logs");
        return toAdminResponsePage(
                transLogRepository.findAllExcludingAccountTypeOrderByCreatedAtDesc(AccountType.BUSINESS, pageable));
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("entryType", convertToMap(transLogRepository.countByEntryTypeGroup()));
        stats.put("transactionType", convertToMap(transLogRepository.countByTransactionTypeGroup()));
        stats.put("currency", convertToMap(transLogRepository.countByCurrencyGroup()));
        stats.put("totalTransactions", transLogRepository.count());
        stats.put("totalCredit", transLogRepository.sumTotalCredit());
        stats.put("totalDebit", transLogRepository.sumTotalDebit());
        stats.put("maxTransactionAmount", transLogRepository.maxTransactionAmount());
        return stats;
    }

    private java.util.Map<String, Long> convertToMap(List<Object[]> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private List<TransLogResponse> toAdminResponses(List<TransLog> logs) {
        Set<String> businessAccountNumbers = findBusinessAccountNumbers(logs);
        return logs.stream()
                .map(log -> TransLogResponse.fromEntityForAdmin(log, businessAccountNumbers))
                .collect(Collectors.toList());
    }

    private Page<TransLogResponse> toAdminResponsePage(Page<TransLog> logs) {
        Set<String> businessAccountNumbers = findBusinessAccountNumbers(logs.getContent());
        return logs.map(log -> TransLogResponse.fromEntityForAdmin(log, businessAccountNumbers));
    }

    private Set<String> findBusinessAccountNumbers(Collection<TransLog> logs) {
        Set<String> accountNumbers = logs.stream()
                .flatMap(log -> Stream.of(log.getAccountNumber(), log.getCounterpartAccount()))
                .filter(accountNumber -> accountNumber != null && !accountNumber.isBlank())
                .collect(Collectors.toSet());
        if (accountNumbers.isEmpty()) {
            return Set.of();
        }
        return accountRepository.findAllByAccountNumberInAndAccountType(accountNumbers, AccountType.BUSINESS).stream()
                .map(Account::getAccountNumber)
                .collect(Collectors.toSet());
    }
}
