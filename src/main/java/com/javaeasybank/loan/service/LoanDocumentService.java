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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 補件文件管理業務邏輯 Service。
 *
 * <p>負責客戶補件流程中的文件生命週期管理：</p>
 * <ul>
 *   <li><b>上傳</b>：驗證所有權與數量上限，透過 {@code FileStorageService} 儲存實體檔案，
 *       並寫入 {@code LoanDocument} 後設資訊。</li>
 *   <li><b>送出</b>：標記補件已全數備妥（寫入 {@code documentsSubmittedAt}），
 *       並以 best-effort 方式通知風控模組。</li>
 *   <li><b>刪除</b>：驗證所有權及送出狀態，清除實體檔案與 DB 記錄。</li>
 *   <li><b>查詢</b>：客戶端驗證所有權；行員端需客戶送出後才顯示。</li>
 * </ul>
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

    // ── 上傳 ─────────────────────────────────────────────────────────

    /**
     * 客戶上傳單份補件文件。
     *
     * <p>執行前檢查：</p>
     * <ol>
     *   <li>申請必須存在，且 {@code customerId} 必須與申請所有人一致。</li>
     *   <li>補件尚未送出（{@code documentsSubmittedAt == null}）。</li>
     *   <li>該申請已上傳文件數量未達上限（每筆最多 5 份）。</li>
     *   <li>{@code documentType} 字串必須對應合法的 {@code LoanDocumentType} 列舉值。</li>
     * </ol>
     *
     * @param applicationId 申請識別碼
     * @param customerId    上傳者的客戶識別碼（從 JWT 取得）
     * @param documentType  文件類型字串（{@code LoanDocumentType} 的 name()）
     * @param file          上傳的文件檔案
     * @return 上傳後的文件資訊 DTO
     * @throws BusinessException 若驗證失敗
     */
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
        documentRepo.save(doc);

        log.info("[Document] 上傳完成 documentId={} applicationId={}", doc.getDocumentId(), applicationId);
        return toResponseDTO(doc);
    }

    // ── 送出補件 ─────────────────────────────────────────────────────

    /**
     * 客戶確認補件已全數備妥並正式送出。
     *
     * <p>寫入 {@code LoanApplication.documentsSubmittedAt}，
     * 行員後台在此之後才可查看並審核文件。
     * 送出後以 best-effort 方式呼叫 {@code LoanRiskClient.attachDocuments}
     * 通知風控模組，失敗不影響送出主流程。</p>
     *
     * @param applicationId 申請識別碼
     * @param customerId    操作者的客戶識別碼
     * @throws BusinessException 若所有權驗證失敗、尚未上傳文件或補件已送出
     */
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

    // ── 刪除 ─────────────────────────────────────────────────────────

    /**
     * 客戶刪除自己上傳的補件文件（補件送出前才允許）。
     *
     * <p>先刪除 {@code FileStorageService} 的實體檔案（失敗僅記錄警告，不中斷），
     * 再移除 DB 記錄。</p>
     *
     * @param documentId 文件識別碼
     * @param customerId 操作者的客戶識別碼
     * @throws BusinessException 若文件不存在、所有權驗證失敗或補件已送出
     */
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

    /**
     * 客戶端查詢補件文件清單（需驗證所有權）。
     *
     * @param applicationId 申請識別碼
     * @param customerId    查詢者的客戶識別碼
     * @return 文件清單，依上傳時間升序排列
     * @throws BusinessException 若申請不存在或所有權驗證失敗
     */
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

    /**
     * 行員端查詢補件文件清單（客戶送出後才顯示）。
     *
     * <p>若客戶尚未送出補件（{@code documentsSubmittedAt == null}），
     * 回傳空清單，前端可顯示「客戶尚未送出補件」提示。</p>
     *
     * @param applicationId 申請識別碼
     * @return 文件清單（依上傳時間升序），或空清單
     * @throws BusinessException 若申請不存在
     */
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

    // ── 工具方法 ─────────────────────────────────────────────────────

    /**
     * 產生格式化識別碼：前綴 + {@code yyyyMMddHHmmss} + 4 位隨機數字。
     *
     * @param prefix 識別碼前綴，例如 {@code "DOC"}
     * @return 格式化的唯一識別碼
     */
    private String generateId(String prefix) {
        String timeStr      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    /**
     * 將 {@code LoanDocument} Entity 轉換為回應 DTO。
     *
     * @param doc 文件 Entity
     * @return 對應的回應 DTO
     */
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
