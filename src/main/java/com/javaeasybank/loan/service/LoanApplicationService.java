//package com.javaeasybank.loan.service;
//
//import com.javaeasybank.common.exception.BusinessException;
//import com.javaeasybank.loan.dto.LoanApplicationRequestDTO;
//import com.javaeasybank.loan.dto.LoanApplicationResponseDTO;
//import com.javaeasybank.loan.dto.LoanContactLogRequestDTO;
//import com.javaeasybank.loan.dto.LoanContactLogResponseDTO;
//import com.javaeasybank.loan.dto.LoanRejectRequestDTO;
//import com.javaeasybank.loan.dto.LoanReviewDetailRequestDTO;
//import com.javaeasybank.loan.dto.LoanReviewDetailResponseDTO;
//import com.javaeasybank.loan.entity.LoanApplication;
//import com.javaeasybank.loan.entity.LoanContactLog;
//import com.javaeasybank.loan.entity.LoanReviewDetail;
//import com.javaeasybank.loan.enums.LoanApplicationStatus;
//import com.javaeasybank.loan.enums.LoanContactStatus;
//import com.javaeasybank.loan.enums.LoanReviewStatus;
//import com.javaeasybank.loan.repository.LoanApplicationRepository;
//import com.javaeasybank.loan.repository.LoanContactLogRepository;
//import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.stream.Collectors;
//
///*
// * 負責處理所有貸款申請相關的核心業務，包含：
// *   - 申請建立與查詢
// *   - 行員聯繫紀錄（同步更新主表最新聯繫狀態）
// *   - 二次填單草稿儲存與送審（同步更新主表狀態）
// *   - 銀行核准 / 拒絕
// *   - 利率計算與期數驗證
// */
//
//@Service
//@Transactional
//public class LoanApplicationService {
//
//    @Autowired
//    private LoanApplicationRepository laRepo;
//
//    @Autowired
//    private LoanContactLogRepository contactLogRepo;
//
//    @Autowired
//    private LoanReviewDetailRepository reviewDetailRepo;
//
//    // ===查詢功能===
//    // 依狀態顯示
//    public List<LoanApplicationResponseDTO> getByStatus(LoanApplicationStatus status) {
//        return laRepo.findByApplicationStatusOrderByCreateTimeDesc(status)
//                .stream()
//                .map(this::toResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ===新增功能===
//    // 新增申請
//    public String insert(LoanApplicationRequestDTO dto) {
//
//        LoanApplication loan = new LoanApplication();
//
//        // 自動產生 applicationId：LA + yyyyMMddHHmmss + 4碼隨機數
//        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
//        loan.setApplicationId("LA" + timeStr + randomSuffix);
//
//        // Case.1 本行會員
//        loan.setCustomerId(dto.getCustomerId());
//
//        // Case.2 非會員
//        loan.setApplicantName(dto.getApplicantName());
//        loan.setApplicantPhone(dto.getApplicantPhone());
//        loan.setApplicantEmail(dto.getApplicantEmail());
//
//        // 申請內容
//        loan.setApplyType(dto.getApplyType());
//        loan.setApplyAmount(dto.getApplyAmount());
//        loan.setApplyPeriod(dto.getApplyPeriod());
//
//        // 後端計算利率
//        loan.setRate(calculateRate(dto.getApplyType(), dto.getApplyPeriod()));
//
//        // 負責行員
//        loan.setEmpId(dto.getEmpId());
//
//        // 後端預設
//        loan.setApplicationStatus(LoanApplicationStatus.PENDING_CONTACT);
//        loan.setCreateTime(LocalDateTime.now());
//
//        laRepo.save(loan);
//
//        return loan.getApplicationId();
//    }
//
//    // ===聯繫紀錄===
//    // 新增聯繫紀錄，同步更新主表最新聯繫狀態
//    public void addContactLog(String applicationId, LoanContactLogRequestDTO dto) {
//
//        LoanApplication loan = laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//
//        // 寫入聯繫紀錄
//        LoanContactLog log = new LoanContactLog();
//        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
//        log.setLogId("CL" + timeStr + randomSuffix);
//        log.setApplicationId(applicationId);
//        log.setEmpId(dto.getEmpId());
//        log.setContactStatus(dto.getContactStatus());
//        log.setContactChannel(dto.getContactChannel());
//        log.setContactTime(LocalDateTime.now());
//        log.setNote(dto.getNote());
//        contactLogRepo.save(log);
//
//        // 同步更新主表最新聯繫狀態
//        loan.setLatestContactStatus(dto.getContactStatus());
//        loan.setLatestContactTime(log.getContactTime());
//
//        // 若主表仍是 PENDING_CONTACT，推進為 IN_CONTACT
//        if (loan.getApplicationStatus() == LoanApplicationStatus.PENDING_CONTACT) {
//            loan.setApplicationStatus(LoanApplicationStatus.IN_CONTACT);
//        }
//
//        laRepo.save(loan);
//    }
//
//
//    // 查某申請的所有聯繫紀錄
//    public List<LoanContactLogResponseDTO> getContactLogs(String applicationId) {
//        return contactLogRepo.findByApplicationIdOrderByContactTimeDesc(applicationId)
//                .stream()
//                .map(this::toContactLogResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ===二次填單===
//    // 儲存草稿：有草稿就覆蓋同一筆，沒有就新建
//    public void saveReviewDetail(String applicationId, LoanReviewDetailRequestDTO dto) {
//
//        // 確認主表存在
//        laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//
//        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
//                .orElse(new LoanReviewDetail());   // 沒有草稿就建新的
//
//        // 若是全新的，產生 PK 並綁定 applicationId
//        if (detail.getReviewId() == null) {
//            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//            String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
//            detail.setReviewId("RD" + timeStr + randomSuffix);
//            detail.setApplicationId(applicationId);
//        }
//
//        // 填入審核內容（覆蓋舊草稿）
//        detail.setConfirmedAmount(dto.getConfirmedAmount());
//        detail.setConfirmedPeriod(dto.getConfirmedPeriod());
//        detail.setConfirmedRate(calculateRate(applicationId, dto.getConfirmedPeriod())); // 後端計算
//        detail.setCollateralNote(dto.getCollateralNote());
//        detail.setEmpId(dto.getEmpId());
//        detail.setReviewTime(LocalDateTime.now());
//        detail.setReviewStatus(LoanReviewStatus.DRAFT);
//
//        reviewDetailRepo.save(detail);
//    }
//
//    // 送審：草稿 → SUBMITTED，主表推進為 PENDING_REVIEW
//    public void submitReview(String applicationId) {
//
//        LoanApplication loan = laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//
//        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
//                .orElseThrow(() -> new BusinessException("尚未建立二次填單草稿，無法送審"));
//
//        // 確認目前是草稿才能送審
//        if (detail.getReviewStatus() != LoanReviewStatus.DRAFT) {
//            throw new BusinessException("此申請已送審，請勿重複操作");
//        }
//
//        // 更新填單狀態
//        detail.setReviewStatus(LoanReviewStatus.SUBMITTED);
//        detail.setSubmittedTime(LocalDateTime.now());
//        reviewDetailRepo.save(detail);
//
//        // 同步更新主表狀態
//        loan.setApplicationStatus(LoanApplicationStatus.PENDING_REVIEW);
//        laRepo.save(loan);
//    }
//
//    // 查填單內容
//    public LoanReviewDetailResponseDTO getReviewDetail(String applicationId) {
//        LoanReviewDetail detail = reviewDetailRepo.findByApplicationId(applicationId)
//                .orElseThrow(() -> new BusinessException("尚未建立二次填單：" + applicationId));
//        return toReviewDetailResponseDTO(detail);
//    }
//
//    // ===風控操作回傳結果===
//    // 核准
//    public void approve(String applicationId) {
//
//        LoanApplication loan = laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//
//        loan.setApplicationStatus(LoanApplicationStatus.APPROVED);
//        laRepo.save(loan);
//    }
//
//    // 拒絕
//    public void reject(String applicationId, LoanRejectRequestDTO dto) {
//
//        LoanApplication loan = laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//
//        loan.setApplicationStatus(LoanApplicationStatus.REJECTED);
//        loan.setEmpId(dto.getEmpId());
//        laRepo.save(loan);
//    }
//
//
//    // 利率計算 — 從 DB 取 applyType，不信任外部傳入
//    private BigDecimal calculateRate(String applicationId, Integer period) {
//        LoanApplication loan = laRepo.findById(applicationId)
//                .orElseThrow(() -> new BusinessException("找不到申請編號：" + applicationId));
//        return calculateRate(loan.getApplyType(), period);
//    }
//
//    // 利率計算 — 直接傳 applyType（insert 時使用）
//    public BigDecimal calculateRate(String applyType, Integer term) {
//
//        if (applyType == null || term == null) {
//            return new BigDecimal("0.03");
//        }
//
//        BigDecimal baseRate;
//        switch (applyType) {
//            case "PERSONAL":
//                baseRate = new BigDecimal("0.04");
//                validateTerm(term, new int[]{12, 24, 36, 48, 60});
//                break;
//            case "CAR":
//                baseRate = new BigDecimal("0.025");
//                validateTerm(term, new int[]{12, 24, 36, 48, 60});
//                break;
//            case "MOTOR":
//                baseRate = new BigDecimal("0.045");
//                validateTerm(term, new int[]{12, 24, 36});
//                break;
//            case "STUDENT":
//                validateTerm(term, new int[]{60, 84, 120});
//                return new BigDecimal("0.015");
//            case "BUSINESS":
//                baseRate = new BigDecimal("0.02");
//                validateTerm(term, new int[]{36, 60, 84});
//                break;
//            case "HOUSE":
//                baseRate = new BigDecimal("0.018");
//                validateTerm(term, new int[]{120, 240, 360, 480});
//                break;
//            case "LAND":
//                baseRate = new BigDecimal("0.028");
//                validateTerm(term, new int[]{120, 180, 240});
//                break;
//            default:
//                baseRate = new BigDecimal("0.03");
//        }
//
//        BigDecimal termRate;
//        switch (term) {
//            case 12:
//                termRate = BigDecimal.ZERO;
//                break;
//            case 24:
//                termRate = new BigDecimal("0.002");
//                break;
//            case 36:
//                termRate = new BigDecimal("0.005");
//                break;
//            case 48:
//                termRate = new BigDecimal("0.008");
//                break;
//            case 60:
//                termRate = new BigDecimal("0.01");
//                break;
//            case 84:
//                termRate = new BigDecimal("0.015");
//                break;
//            case 120:
//                termRate = BigDecimal.ZERO;
//                break;
//            case 180:
//                termRate = new BigDecimal("0.002");
//                break;
//            case 240:
//                termRate = new BigDecimal("0.004");
//                break;
//            case 360:
//                termRate = new BigDecimal("0.006");
//                break;
//            case 480:
//                termRate = new BigDecimal("0.008");
//                break;
//            default:
//                termRate = BigDecimal.ZERO;
//        }
//
//        return baseRate.add(termRate);
//    }
//
//    // 期數驗證
//    private void validateTerm(int term, int[] allowed) {
//        for (int t : allowed) {
//            if (t == term) return;
//        }
//        throw new BusinessException("此貸款種類不支援該期數：" + term);
//    }
//
//    // Entity → LoanApplicationResponseDTO
//    private LoanApplicationResponseDTO toResponseDTO(LoanApplication loan) {
//        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
//        dto.setApplicationId(loan.getApplicationId());
//        dto.setCustomerId(loan.getCustomerId());
//        dto.setApplicantName(loan.getApplicantName());
//        dto.setApplicantPhone(loan.getApplicantPhone());
//        dto.setApplicantEmail(loan.getApplicantEmail());
//        dto.setApplyType(loan.getApplyType());
//        dto.setApplyAmount(loan.getApplyAmount());
//        dto.setApplyPeriod(loan.getApplyPeriod());
//        dto.setRate(loan.getRate());
//        dto.setApplicationStatus(loan.getApplicationStatus());
//        dto.setEmpId(loan.getEmpId());
//        dto.setCreateTime(loan.getCreateTime());
//        dto.setLatestContactStatus(loan.getLatestContactStatus());
//        dto.setLatestContactTime(loan.getLatestContactTime());
//        return dto;
//    }
//
//    // Entity → LoanContactLogResponseDTO
//    private LoanContactLogResponseDTO toContactLogResponseDTO(LoanContactLog log) {
//        LoanContactLogResponseDTO dto = new LoanContactLogResponseDTO();
//        dto.setLogId(log.getLogId());
//        dto.setApplicationId(log.getApplicationId());
//        dto.setEmpId(log.getEmpId());
//        dto.setContactStatus(log.getContactStatus());
//        dto.setContactChannel(log.getContactChannel());
//        dto.setContactTime(log.getContactTime());
//        dto.setNote(log.getNote());
//        return dto;
//    }
//
//    // Entity → LoanReviewDetailResponseDTO
//    private LoanReviewDetailResponseDTO toReviewDetailResponseDTO(LoanReviewDetail detail) {
//        LoanReviewDetailResponseDTO dto = new LoanReviewDetailResponseDTO();
//        dto.setReviewId(detail.getReviewId());
//        dto.setApplicationId(detail.getApplicationId());
//        dto.setConfirmedAmount(detail.getConfirmedAmount());
//        dto.setConfirmedPeriod(detail.getConfirmedPeriod());
//        dto.setConfirmedRate(detail.getConfirmedRate());
//        dto.setCollateralNote(detail.getCollateralNote());
//        dto.setEmpId(detail.getEmpId());
//        dto.setReviewTime(detail.getReviewTime());
//        dto.setReviewStatus(detail.getReviewStatus());
//        dto.setSubmittedTime(detail.getSubmittedTime());
//        dto.setReviewNote(detail.getReviewNote());
//        return dto;
//    }
//}