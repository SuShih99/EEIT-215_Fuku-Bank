package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.loan.client.LoanRiskClient;
import com.javaeasybank.loan.dto.requests.*;
import com.javaeasybank.loan.dto.response.LoanApplicationResponseDTO;
import com.javaeasybank.loan.dto.response.LoanContactLogResponseDTO;
import com.javaeasybank.loan.dto.response.LoanReviewDetailResponseDTO;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanContactLog;
import com.javaeasybank.loan.entity.LoanReviewDetail;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import com.javaeasybank.loan.enums.LoanReviewStatus;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanContactLogRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 負責處理所有貸款申請相關的核心業務，包含：
 *   - 申請建立與查詢
 *   - 行員聯繫紀錄（同步更新主表最新聯繫狀態）
 *   - 二次填單草稿儲存與送審（同步更新主表狀態）
 *   - 利率規則回傳 (前端根據規則自行計算)
 */

@Service
@Transactional
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository laRepo;

    @Autowired
    private LoanContactLogRepository contactLogRepo;

    @Autowired
    private LoanReviewDetailRepository reviewDetailRepo;

    @Autowired
    private LoanRiskClient loanRiskClient;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    // ===查詢功能===
    // 依狀態顯示
    public List<LoanApplicationResponseDTO> getByStatus(LoanApplicationStatus status) {
        return laRepo.findByApplicationStatusOrderByCreateTimeDesc(status)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 查詢當前登入客戶所有申請
    public List<LoanApplicationResponseDTO> getMyApplications(String customerId) {
        return laRepo.findByCustomerIdOrderByCreateTimeDesc(customerId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ===新增功能===

    /// 會員申請
    public String insertMember(String customerId, LoanMemberRequestDTO dto) {
        LoanApplication loan = buildBaseLoan();
        loan.setCustomerId(customerId);
        fillLoanContent(loan, dto.getApplyType(), dto.getApplyAmount(),
                dto.getApplyPeriod(), dto.getRate());
        laRepo.save(loan);
        return loan.getApplicationId();
    }

    // 共用：產生格式化 ID（前綴 + yyyyMMddHHmmss + 4 位亂數）
    private String generateId(String prefix) {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + timeStr + randomSuffix;
    }

    // 產生貸款申請物件 + 預設狀態
    private LoanApplication buildBaseLoan() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationId(generateId("LA"));
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_CONTACT);
        loan.setCreateTime(LocalDateTime.now());
        return loan;
    }

    // 共用：填申請內容
    private void fillLoanContent(LoanApplication loan, String applyType,
                                 BigDecimal applyAmount, Integer applyPeriod,
                                 BigDecimal rate) {
        loan.setApplyType(applyType);
        loan.setApplyAmount(applyAmount);
        loan.setApplyPeriod(applyPeriod);
        loan.setRate(rate);
    }

    // ===聯繫紀錄===
    // 新增聯繫紀錄，同步更新主表最新聯繫狀態
    public void addContactLog(String applicationId, LoanContactLogRequestDTO dto) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
        LoanContactStatus contactStatus = LoanContactStatus.valueOf(dto.getContactStatus());
        LoanContactChannel contactChannel = LoanContactChannel.valueOf(dto.getContactChannel());

        // 寫入聯繫紀錄
        LoanContactLog log = new LoanContactLog();
        log.setLogId(generateId("CL"));
        log.setApplicationId(applicationId);
        log.setEmpId(dto.getEmpId());
        log.setContactStatus(contactStatus);
        log.setContactChannel(contactChannel);
        log.setContactTime(LocalDateTime.now());
        log.setNote(dto.getNote());
        contactLogRepo.save(log);

        // 同步更新主表最新聯繫狀態
        loan.setLatestContactStatus(contactStatus);
        loan.setLatestContactTime(log.getContactTime());

        // 若主表仍是 PENDING_CONTACT，推進為 IN_CONTACT
        if (loan.getApplicationStatus() == LoanApplicationStatus.PENDING_CONTACT) {
            loan.setApplicationStatus(LoanApplicationStatus.IN_CONTACT);
        }
        // 客戶放棄時，主表推進為 CANCELLED
        if (contactStatus == LoanContactStatus.DECLINED) {
            loan.setApplicationStatus(LoanApplicationStatus.CANCELLED);
        }

        laRepo.save(loan);
    }


    // 查某申請的所有聯繫紀錄
    public List<LoanContactLogResponseDTO> getContactLogs(String applicationId) {
        return contactLogRepo.findByApplicationIdOrderByContactTimeDesc(applicationId)
                .stream()
                .map(this::toContactLogResponseDTO)
                .collect(Collectors.toList());
    }

    // ===二次填單===
    // 儲存草稿：有草稿就覆蓋同一筆，沒有就新建
    // 送審後不可修改
    public void saveReviewDetail(String applicationId, LoanReviewDetailRequestDTO dto) {

        // 確認主表存在
        laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 沒有草稿就建新的
        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElse(new LoanReviewDetail());

        // 已送審的填單不可再修改
        if (detail.getReviewId() != null
                && detail.getReviewStatus() == LoanReviewStatus.SUBMITTED) {
            throw new BusinessException("此申請已送審，無法修改填單內容");
        }

        // 若是全新的，產生 PK 並綁定 applicationId
        if (detail.getReviewId() == null) {
            detail.setReviewId(generateId("RD"));
            detail.setApplicationId(applicationId);
        }

        // 填入審核內容（覆蓋舊草稿）
        detail.setConfirmedAmount(dto.getConfirmedAmount());
        detail.setConfirmedPeriod(dto.getConfirmedPeriod());
        detail.setConfirmedRate(dto.getConfirmedRate());
        detail.setCollateralNote(dto.getCollateralNote());
        detail.setEmpId(dto.getEmpId());
        detail.setReviewTime(LocalDateTime.now());
        detail.setReviewStatus(LoanReviewStatus.DRAFT);

        reviewDetailRepo.save(detail);
    }

    // 送審：草稿 → SUBMITTED，主表推進為 PENDING_REVIEW
    // 申請狀態必須是 IN_CONTACT
    public void submitReview(String applicationId) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 狀態前置檢查
        if (loan.getApplicationStatus() != LoanApplicationStatus.IN_CONTACT) {
            throw new BusinessException("此申請目前狀態無法送審");
        }

        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("尚未建立二次填單草稿，無法送審"));

        // 確認目前是草稿才能送審
        if (detail.getReviewStatus() != LoanReviewStatus.DRAFT) {
            throw new BusinessException("此申請已送審，請勿重複操作");
        }

        // 更新填單狀態
        detail.setReviewStatus(LoanReviewStatus.SUBMITTED);
        detail.setSubmittedTime(LocalDateTime.now());
        reviewDetailRepo.save(detail);

        // 同步更新主表狀態
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_REVIEW);
        laRepo.save(loan);

        // 送交資料給風控
        // 失敗呼叫BusinessException
        loanRiskClient.submitForReview(buildRiskRequest(loan, detail));
    }

    // 送風控的DTO (申請資料+二次填單)
    private LoanRiskRequestDTO buildRiskRequest(LoanApplication loan, LoanReviewDetail detail) {
        LoanRiskRequestDTO dto = new LoanRiskRequestDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 補入 cif 供風控模組對照顯示用
        String cif = customerProfileRepository.findById(loan.getCustomerId())
                .map(p -> p.getCif())
                .orElse(null);
        dto.setCif(cif);
        dto.setApplyType(loan.getApplyType());
        dto.setConfirmedAmount(detail.getConfirmedAmount());
        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
        dto.setConfirmedRate(detail.getConfirmedRate());
        dto.setCollateralNote(detail.getCollateralNote());
        dto.setEmpId(detail.getEmpId());
        dto.setSubmittedTime(detail.getSubmittedTime());
        return dto;
    }

    // 風控審核完成後傳回，更新主表狀態為 APPROVED 或 REJECTED
    // callerModule 必須是 "RISK"；可接受的目標狀態只有 APPROVED / REJECTED
    public void handleStatusCallback(String applicationId, LoanStatusCallbackRequestDTO dto) {

        LoanApplication loan = laRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));

        // 來源模組驗證
        if (!"RISK".equals(dto.getCallerModule())) {
            throw new BusinessException("此 callback 僅接受 RISK 模組呼叫");
        }

        // 狀態合法性驗證：當前必須是 PENDING_REVIEW
        if (loan.getApplicationStatus() != LoanApplicationStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "申請目前狀態為 " + loan.getApplicationStatus() + "，無法套用風控回調");
        }

        // 目標狀態只允許 APPROVED / REJECTED
        if (dto.getNewStatus() != LoanApplicationStatus.APPROVED
                && dto.getNewStatus() != LoanApplicationStatus.REJECTED) {
            throw new BusinessException("風控回調目標狀態不合法：" + dto.getNewStatus());
        }

        loan.setApplicationStatus(dto.getNewStatus());
        loan.setUpdateTime(LocalDateTime.now());
        laRepo.save(loan);
    }

    // 查填單內容
    public LoanReviewDetailResponseDTO getReviewDetail(String applicationId) {
        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException("尚未建立二次填單：" + applicationId));
        return toReviewDetailResponseDTO(detail);
    }

    // 置頂查詢：曾被外部模組（風控、帳戶）異動過狀態的申請，依更新時間降序
    public List<LoanApplicationResponseDTO> getRecentlyUpdated() {
        return laRepo.findByUpdateTimeIsNotNullOrderByUpdateTimeDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ===利率規則===
    // 回傳利率規則表，供前端頁面載入時抓取
    // 前端依此規則：
    //   1. 根據貸款種類顯示合法期數下拉選單
    //   2. 選完期數自動計算並顯示利率
    //   3. 送出時將算好的利率一併傳入後端

    public Map<String, Object> getRateRules() {

        // 各貸款種類：基礎利率 + 合法期數（fixedRate 為 true 表示固定利率，不加 termRate）
        Map<String, Object> types = new LinkedHashMap<>();
        types.put("PERSONAL", Map.of(
                "baseRate", new BigDecimal("0.04"),
                "periods", List.of(12, 24, 36, 48, 60)
        ));
        types.put("CAR", Map.of(
                "baseRate", new BigDecimal("0.025"),
                "periods", List.of(12, 24, 36, 48, 60)
        ));
        types.put("MOTOR", Map.of(
                "baseRate", new BigDecimal("0.045"),
                "periods", List.of(12, 24, 36)
        ));
        types.put("STUDENT", Map.of(
                "baseRate", new BigDecimal("0.015"),
                "periods", List.of(60, 84, 120),
                "fixedRate", true   // 固定利率，不加 termRate
        ));
        types.put("BUSINESS", Map.of(
                "baseRate", new BigDecimal("0.02"),
                "periods", List.of(36, 60, 84)
        ));
        types.put("HOUSE", Map.of(
                "baseRate", new BigDecimal("0.018"),
                "periods", List.of(120, 240, 360, 480)
        ));
        types.put("LAND", Map.of(
                "baseRate", new BigDecimal("0.028"),
                "periods", List.of(120, 180, 240)
        ));

        // 期數加碼對照表
        Map<String, BigDecimal> termRates = new LinkedHashMap<>();
        termRates.put("12", BigDecimal.ZERO);
        termRates.put("24", new BigDecimal("0.002"));
        termRates.put("36", new BigDecimal("0.005"));
        termRates.put("48", new BigDecimal("0.008"));
        termRates.put("60", new BigDecimal("0.01"));
        termRates.put("84", new BigDecimal("0.015"));
        termRates.put("120", BigDecimal.ZERO);
        termRates.put("180", new BigDecimal("0.002"));
        termRates.put("240", new BigDecimal("0.004"));
        termRates.put("360", new BigDecimal("0.006"));
        termRates.put("480", new BigDecimal("0.008"));

        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("types", types);
        rules.put("termRates", termRates);

        return rules;
    }

    // ===DTO轉換===
    // Entity → LoanApplicationResponseDTO
    private LoanApplicationResponseDTO toResponseDTO(LoanApplication loan) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        dto.setApplicationId(loan.getApplicationId());
        dto.setCustomerId(loan.getCustomerId());
        // 用 customerId 查出 cif 供前端顯示（Primary Key 查詢，效能无淣）
        String cif = customerProfileRepository.findById(loan.getCustomerId())
                .map(p -> p.getCif())
                .orElse(null);
        dto.setCif(cif);
        dto.setApplyType(loan.getApplyType());
        dto.setApplyAmount(loan.getApplyAmount());
        dto.setApplyPeriod(loan.getApplyPeriod());
        dto.setRate(loan.getRate());
        dto.setApplicationStatus(loan.getApplicationStatus());
        dto.setCreateTime(loan.getCreateTime());
        dto.setLatestContactStatus(loan.getLatestContactStatus());
        dto.setLatestContactTime(loan.getLatestContactTime());
        return dto;
    }

    // Entity → LoanContactLogResponseDTO
    private LoanContactLogResponseDTO toContactLogResponseDTO(LoanContactLog log) {
        LoanContactLogResponseDTO dto = new LoanContactLogResponseDTO();
        dto.setLogId(log.getLogId());
        dto.setApplicationId(log.getApplicationId());
        dto.setEmpId(log.getEmpId());
        dto.setContactStatus(log.getContactStatus());
        dto.setContactChannel(log.getContactChannel());
        dto.setContactTime(log.getContactTime());
        dto.setNote(log.getNote());
        return dto;
    }

    // Entity → LoanReviewDetailResponseDTO
    private LoanReviewDetailResponseDTO toReviewDetailResponseDTO(LoanReviewDetail detail) {
        LoanReviewDetailResponseDTO dto = new LoanReviewDetailResponseDTO();
        dto.setReviewId(detail.getReviewId());
        dto.setApplicationId(detail.getApplicationId());
        dto.setConfirmedAmount(detail.getConfirmedAmount());
        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
        dto.setConfirmedRate(detail.getConfirmedRate());
        dto.setCollateralNote(detail.getCollateralNote());
        dto.setEmpId(detail.getEmpId());
        dto.setReviewTime(detail.getReviewTime());
        dto.setReviewStatus(detail.getReviewStatus());
        dto.setSubmittedTime(detail.getSubmittedTime());
        dto.setReviewNote(detail.getReviewNote());
        return dto;
    }

}