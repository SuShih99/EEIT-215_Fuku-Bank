package com.javaeasybank.loan.service;

/**
 * 程式說明：
 * - 貸款文件業務服務。
 * - 負責文件上傳、補件批次、文件審核狀態、合約文件與查詢回應組裝。
 */

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.FileStorageService;
import com.javaeasybank.loan.client.LoanRiskClient;
import com.javaeasybank.loan.dto.requests.LoanDocumentInfoDTO;
import com.javaeasybank.loan.dto.response.LoanDocumentResponseDTO;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanDocument;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanDocumentType;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

// 補件文件管理業務邏輯 Service
@Slf4j
/**
 * 貸款補件文件服務。
 * 管理文件上傳、刪除、送出補件批次，並在補件完成後通知風控重新審核。
 */
@Service
@Transactional
public class LoanDocumentService {

    private static final String BATCH_INITIAL = "INITIAL";
    private static final String BATCH_SUPPLEMENT = "SUPPLEMENT";

    @Autowired
    private LoanDocumentRepository documentRepo;

    @Autowired
    private LoanApplicationRepository loanApplicationRepo;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LoanRiskClient loanRiskClient;

    // ── 上傳 ─────────────────────────────────────────────────────────

    // 客戶上傳單份補件文件
    public LoanDocumentResponseDTO upload(String applicationId,
                                          String customerId,
                                          String documentType,
                                          MultipartFile file) {

        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 所有權驗證：只能上傳自己申請的補件
        if (!loan.getCustomerId().equals(customerId)) {
            throw new BusinessException("無權操作此申請的文件");
        }

        // 已送出補件後不可再上傳
        if (loan.getDocumentsSubmittedAt() != null) {
            throw new BusinessException("文件已送出，如需補充請聯繫行員");
        }
        if (loan.getApplicationStatus() != LoanApplicationStatus.IN_CONTACT
                && loan.getApplicationStatus() != LoanApplicationStatus.RETURNED) {
            throw new BusinessException("此申請目前狀態不可上傳文件");
        }

        String batchType = resolveWritableBatchType(loan);
        Integer batchNo = resolveWritableBatchNo(loan);

        // 數量上限：每批次最多 5 份文件
        long existingCount = documentRepo.countByApplicationIdAndDocumentBatchTypeAndDocumentBatchNo(
                applicationId, batchType, batchNo);
        if (existingCount >= 5) {
            throw new BusinessException("每批次最多上傳 5 份文件，目前已達上限");
        }

        LoanDocumentType type;
        try {
            type = LoanDocumentType.valueOf(documentType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不合法的文件類型：" + documentType);
        }

        String fileUrl = fileStorageService.store(file, "loan-documents/" + applicationId);

        LoanDocument doc = new LoanDocument();
        doc.setDocumentId(generateId("DOC"));
        doc.setApplicationId(applicationId);
        doc.setDocumentType(type);
        doc.setFileUrl(fileUrl);
        doc.setOriginalName(file.getOriginalFilename());
        doc.setUploadedBy(customerId);
        doc.setUploadTime(LocalDateTime.now());
        doc.setDocumentBatchType(batchType);
        doc.setDocumentBatchNo(batchNo);
        documentRepo.save(doc);

        log.info("[Document] 上傳完成 documentId={} applicationId={}", doc.getDocumentId(), applicationId);
        return toResponseDTO(doc);
    }

    // ── 送出補件 ─────────────────────────────────────────────────────

    // 客戶確認補件已全數備妥並正式送出
    public void submitDocuments(String applicationId, String customerId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        if (!loan.getCustomerId().equals(customerId)) {
            throw new BusinessException("無權操作此申請");
        }
        String batchType = resolveWritableBatchType(loan);
        Integer batchNo = resolveWritableBatchNo(loan);
        if (documentRepo.countByApplicationIdAndDocumentBatchTypeAndDocumentBatchNo(applicationId, batchType, batchNo) == 0) {
            throw new BusinessException("尚未上傳任何文件，無法送出");
        }
        if (loan.getDocumentsSubmittedAt() != null) {
            throw new BusinessException("補件已送出，如需補充請聯繫行員");
        }
        LoanApplicationStatus status = loan.getApplicationStatus();
        if (status != LoanApplicationStatus.IN_CONTACT && status != LoanApplicationStatus.RETURNED) {
            throw new BusinessException("此申請目前狀態不可送出文件");
        }

        boolean supplement = status == LoanApplicationStatus.RETURNED;
        LocalDateTime submittedAt = LocalDateTime.now();
        List<LoanDocument> currentBatchDocs = documentRepo
                .findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
                        applicationId, batchType, batchNo);
        currentBatchDocs.forEach(doc -> doc.setSubmittedAt(submittedAt));
        documentRepo.saveAll(currentBatchDocs);
        if (supplement) {
            loan.setApplicationStatus(LoanApplicationStatus.PENDING_REVIEW);
        }
        loan.setDocumentsSubmittedAt(submittedAt);
        loanApplicationRepo.save(loan);
        log.info("[Document] 文件送出 applicationId={} customerId={} status={}", applicationId, customerId, status);

        if (!supplement) {
            return;
        }

        // 退回補件送出後通知風控模組（失敗不影響送出結果）。
        // 風控後台審核不做批次隔離，需看見此申請所有已上傳文件。
        try {
            List<LoanDocumentInfoDTO> docInfos = documentRepo
                    .findByApplicationIdOrderByUploadTimeAsc(applicationId)
                    .stream()
                    .map(d -> new LoanDocumentInfoDTO(
                            d.getDocumentId(),
                            d.getDocumentType().name(),
                            d.getFileUrl(),
                            d.getOriginalName()))
                    .collect(Collectors.toList());
            loanRiskClient.attachDocuments(applicationId, docInfos);
        } catch (Exception e) {
            log.warn("[Document] 補件通知風控失敗（不影響送出） applicationId={} err={}", applicationId, e.getMessage());
        }
    }

    // ── 刪除 ─────────────────────────────────────────────────────────

    // 客戶刪除自己上傳的補件文件（補件送出前才允許）
    public void delete(String documentId, String customerId) {
        LoanDocument doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new BusinessException("找不到文件：" + documentId));

        // 所有權驗證：只能刪除自己上傳的文件
        if (!doc.getUploadedBy().equals(customerId)) {
            throw new BusinessException("無權刪除此文件");
        }

        // 已送出補件後不可再刪除
        LoanApplication loan = loanApplicationRepo.findById(doc.getApplicationId())
                .orElseThrow(() -> new BusinessException("找不到對應的申請"));
        if (loan.getDocumentsSubmittedAt() != null) {
            throw new BusinessException("補件已送出，如需變更請聯繫行員");
        }
        String batchType = resolveWritableBatchType(loan);
        Integer batchNo = resolveWritableBatchNo(loan);
        if (!batchType.equals(safeBatchType(doc.getDocumentBatchType()))
                || !batchNo.equals(safeBatchNo(doc.getDocumentBatchNo()))) {
            throw new BusinessException("只能刪除目前批次的文件");
        }

        // 刪除實體檔案（失敗不中斷，僅記錄警告）
        try {
            fileStorageService.delete(doc.getFileUrl());
        } catch (Exception e) {
            log.warn("[Document] 實體檔案刪除失敗，繼續移除 DB 記錄 documentId={} err={}", documentId, e.getMessage());
        }

        documentRepo.deleteById(documentId);
        log.info("[Document] 刪除完成 documentId={}", documentId);
    }

    // ── 查詢 ─────────────────────────────────────────────────────────

    // 客戶端查詢補件文件清單（需驗證所有權）
    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDTO> getByApplicationId(String applicationId, String customerId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        if (!loan.getCustomerId().equals(customerId)) {
            throw new BusinessException("無權存取此申請的文件");
        }
        BatchScope scope = resolveVisibleBatch(loan);
        return documentRepo.findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
                        applicationId, scope.batchType(), scope.batchNo())
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 行員端查詢補件文件清單（客戶送出後才顯示）
    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDTO> getByApplicationId(String applicationId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        // 尚未送出 → 回傳空列表，前端顯示「客戶尚未送出補件」
        if (loan.getDocumentsSubmittedAt() == null) {
            return java.util.Collections.emptyList();
        }
        BatchScope scope = resolveVisibleBatch(loan);
        return documentRepo.findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
                        applicationId, scope.batchType(), scope.batchNo())
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    // 產生格式化識別碼：前綴 + yyyyMMddHHmmss + 4 位隨機數字
    private String generateId(String prefix) {
        String timeStr      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    // 將 LoanDocument Entity 轉換為回應 DTO
    private LoanDocumentResponseDTO toResponseDTO(LoanDocument doc) {
        LoanDocumentResponseDTO dto = new LoanDocumentResponseDTO();
        dto.setDocumentId(doc.getDocumentId());
        dto.setApplicationId(doc.getApplicationId());
        dto.setDocumentType(doc.getDocumentType());
        dto.setFileUrl(doc.getFileUrl());
        dto.setOriginalName(doc.getOriginalName());
        dto.setUploadedBy(doc.getUploadedBy());
        dto.setUploadTime(doc.getUploadTime());
        dto.setDocumentBatchType(safeBatchType(doc.getDocumentBatchType()));
        dto.setDocumentBatchNo(safeBatchNo(doc.getDocumentBatchNo()));
        dto.setSubmittedAt(doc.getSubmittedAt());
        return dto;
    }

    private String resolveWritableBatchType(LoanApplication loan) {
        return switch (loan.getApplicationStatus()) {
            case IN_CONTACT -> BATCH_INITIAL;
            case RETURNED -> BATCH_SUPPLEMENT;
            default -> throw new BusinessException("此申請目前狀態不可上傳文件");
        };
    }

    private Integer resolveWritableBatchNo(LoanApplication loan) {
        return loan.getApplicationStatus() == LoanApplicationStatus.RETURNED
                ? safeBatchNo(loan.getCurrentSupplementBatchNo())
                : 0;
    }

    private BatchScope resolveVisibleBatch(LoanApplication loan) {
        if (loan.getApplicationStatus() == LoanApplicationStatus.RETURNED
                || safeBatchNo(loan.getCurrentSupplementBatchNo()) > 0) {
            return new BatchScope(BATCH_SUPPLEMENT, safeBatchNo(loan.getCurrentSupplementBatchNo()));
        }
        return new BatchScope(BATCH_INITIAL, 0);
    }

    private Integer safeBatchNo(Integer batchNo) {
        return batchNo == null ? 0 : batchNo;
    }

    private String safeBatchType(String batchType) {
        return batchType == null ? BATCH_INITIAL : batchType;
    }

    private record BatchScope(String batchType, Integer batchNo) {
    }
}
