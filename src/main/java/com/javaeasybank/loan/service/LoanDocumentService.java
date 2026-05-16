package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.FileStorageService;
import com.javaeasybank.loan.client.LoanRiskClient;
import com.javaeasybank.loan.dto.requests.LoanDocumentInfoDTO;
import com.javaeasybank.loan.dto.response.LoanDocumentResponseDTO;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanDocument;
import com.javaeasybank.loan.enums.LoanDocumentType;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/*
 * 補件文件管理（預留，補件功能暫未啟用）
 *   - 客戶上傳補件（MultipartFile → FileStorageService → 寫 loan_document）
 *   - 查詢指定申請的文件清單
 */
@Slf4j
@Service
@Transactional
public class LoanDocumentService {

    @Autowired
    private LoanDocumentRepository documentRepo;

    @Autowired
    private LoanApplicationRepository loanApplicationRepo;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LoanRiskClient loanRiskClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===客戶上傳補件===

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
            throw new BusinessException("補件已送出，如需補充請聯繫行員");
        }

        // 數量上限：每筆申請最多 5 份文件
        long existingCount = documentRepo.countByApplicationId(applicationId);
        if (existingCount >= 5) {
            throw new BusinessException("每筆申請最多上傳 5 份文件，目前已達上限");
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
        try {
            insertDocument(doc);
        } catch (RuntimeException e) {
            try {
                fileStorageService.delete(fileUrl);
            } catch (Exception cleanupError) {
                log.warn("[Document] DB 寫入失敗後刪除檔案也失敗 documentId={} fileUrl={} err={}",
                        doc.getDocumentId(), fileUrl, cleanupError.getMessage());
            }
            throw e;
        }

        log.info("[Document] 上傳完成 documentId={} applicationId={}", doc.getDocumentId(), applicationId);
        return toResponseDTO(doc);
    }

    // ===客戶送出補件===

    public void submitDocuments(String applicationId, String customerId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        if (!loan.getCustomerId().equals(customerId)) {
            throw new BusinessException("無權操作此申請");
        }
        if (documentRepo.countByApplicationId(applicationId) == 0) {
            throw new BusinessException("尚未上傳任何文件，無法送出");
        }
        if (loan.getDocumentsSubmittedAt() != null) {
            throw new BusinessException("補件已送出，如需補充請聯繫行員");
        }

        loan.setDocumentsSubmittedAt(LocalDateTime.now());
        loanApplicationRepo.save(loan);
        log.info("[Document] 補件送出 applicationId={} customerId={}", applicationId, customerId);

        // 非同步通知風控模組（失敗不影響送出結果）
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

    // ===客戶刪除補件===

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

        // 刪除實體檔案（失敗不中斷，僅記錄）
        try {
            fileStorageService.delete(doc.getFileUrl());
        } catch (Exception e) {
            log.warn("[Document] 實體檔案刪除失敗，繼續移除 DB 記錄 documentId={} err={}", documentId, e.getMessage());
        }

        documentRepo.deleteById(documentId);
        log.info("[Document] 刪除完成 documentId={}", documentId);
    }

    // ===查詢===

    // 客戶端：驗證申請屬於該客戶才可查
    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDTO> getByApplicationId(String applicationId, String customerId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        if (!loan.getCustomerId().equals(customerId)) {
            throw new BusinessException("無權存取此申請的文件");
        }
        return documentRepo.findByApplicationIdOrderByUploadTimeAsc(applicationId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 行員端：客戶送出後才顯示文件
    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDTO> getByApplicationId(String applicationId) {
        LoanApplication loan = loanApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        // 尚未送出 → 回傳空列表，前端顯示「客戶尚未送出補件」
        if (loan.getDocumentsSubmittedAt() == null) {
            return java.util.Collections.emptyList();
        }
        return documentRepo.findByApplicationIdOrderByUploadTimeAsc(applicationId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ===工具方法===

    private String generateId(String prefix) {
        String timeStr      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    private void insertDocument(LoanDocument doc) {
        jdbcTemplate.update("""
                INSERT INTO loan_document
                    (application_id, document_type, file_url, original_name, upload_time, uploaded_by, document_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                doc.getApplicationId(),
                doc.getDocumentType().name(),
                doc.getFileUrl(),
                doc.getOriginalName(),
                doc.getUploadTime(),
                doc.getUploadedBy(),
                doc.getDocumentId());
    }

    private LoanDocumentResponseDTO toResponseDTO(LoanDocument doc) {
        LoanDocumentResponseDTO dto = new LoanDocumentResponseDTO();
        dto.setDocumentId(doc.getDocumentId());
        dto.setApplicationId(doc.getApplicationId());
        dto.setDocumentType(doc.getDocumentType());
        dto.setFileUrl(doc.getFileUrl());
        dto.setOriginalName(doc.getOriginalName());
        dto.setUploadedBy(doc.getUploadedBy());
        dto.setUploadTime(doc.getUploadTime());
        return dto;
    }
}
