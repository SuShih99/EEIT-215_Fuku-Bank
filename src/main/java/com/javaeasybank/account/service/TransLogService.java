package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.repository.TransLogRepository;
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
 * 交易紀錄業務邏輯服務層。
 * 處理交易紀錄的查詢操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransLogService {

    private final TransLogRepository transLogRepository;

    /**
     * 根據參考 ID 查詢交易紀錄列表。
     *
     * @param referenceId 業務交易參考 ID。
     * @return 包含交易紀錄響應的列表。
     */
    @Transactional(readOnly = true)
    public List<TransLogResponse> getByReferenceId(String referenceId) {
        log.debug("Fetching trans logs by referenceId: {}", referenceId);
        return transLogRepository.findByReferenceId(referenceId).stream()
                .map(TransLogResponse::fromEntity)
                .collect(Collectors.toList());
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
        return transLogRepository.findByAccountInvolved(accountNumber, pageable)
                .map(TransLogResponse::fromEntity);
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
        return transLogRepository.findByCustomerId(customerId, pageable)
                .map(TransLogResponse::fromEntity);
    }

    /**
     * 根據客戶 ID 和日期範圍查詢該客戶名下所有帳戶的交易紀錄分頁列表。
     *
     * @param customerId 客戶 ID。
     * @param startDate  起始日期時間。
     * @param endDate    結束日期時間。
     * @param pageable   分頁資訊。
     * @return 包含交易紀錄響應的分頁列表。
     */
    @Transactional(readOnly = true)
    public Page<TransLogResponse> getByCustomerIdAndDateRange(String customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching trans logs by customerId: {} and date range: {} to {}", customerId, startDate, endDate);
        return transLogRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate, pageable)
                .map(TransLogResponse::fromEntity);
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
        return transLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(TransLogResponse::fromEntity);
    }
}