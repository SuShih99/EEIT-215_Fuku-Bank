package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.request.RiskReviewRequest;
import com.javaeasybank.risk.enums.FundSource;
import com.javaeasybank.risk.enums.Occupation;
import com.javaeasybank.risk.utils.CreditMockUtils;
import com.javaeasybank.risk.enums.RiskLevel;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import com.javaeasybank.risk.repository.CustomerCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CreditScoreService {

    private static final int EXTERNAL_SCORE_MIN = 300;
    private static final int EXTERNAL_SCORE_RANGE = 500; // 800 - 300

    private final CustomerCreditRepository ccRepos;

    @Transactional
    public CustomerCreditInfo initializeCreditInfo(String customerId, LocalDate birthday, Occupation occupation, BigDecimal annualIncome, FundSource fundSource, Boolean isPep) {
        CustomerCreditInfo ccInfo = new CustomerCreditInfo();
        ccInfo.setCustomerId(customerId);
        ccInfo.setOccupation(occupation);
        ccInfo.setAnnualIncome(annualIncome);
        ccInfo.setFundSource(fundSource);

        // 💡 寫入開戶拿到的真實資料 (防呆處理)
        ccInfo.setIsPep(isPep != null ? isPep : false);
        //ccInfo.setJobTitle(job); // 儲存詳細職稱作未來稽核(KYC)使用
        CreditMockUtils.fillMissingFields(ccInfo, birthday);
        score(ccInfo);
        return ccRepos.save(ccInfo);
    }

    /**
     * 對已存在的 CustomerCreditInfo 重新評分（例如資料更新後）
     */
    @Transactional
    public CustomerCreditInfo rescore(String customerId) {
        CustomerCreditInfo ccInfo = ccRepos.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("CustomerCreditInfo not found: " + customerId));
        score(ccInfo);
        log.info("[CreditScore] 評分結果 customerId={} finalScore={} riskLevel={}",
                customerId, ccInfo.getFinalScore(), ccInfo.getRiskLevel());
        return ccRepos.save(ccInfo);
    }

    /**
     * 計算 finalScore 並設定 riskLevel，直接修改傳入的 info 物件
     */
    private void score(CustomerCreditInfo ccInfo) {
        int total = scoreExternalCredit(ccInfo.getExternalScore())
                + scoreDebtRatio(ccInfo.getAnnualIncome(), ccInfo.getOtherBankDebt())
                + scoreOccupation(ccInfo.getOccupation())
                + scoreRealEstate(ccInfo.getHasRealEstate())
                + scoreFundSource(ccInfo.getFundSource());

        ccInfo.setFinalScore(total);
        ccInfo.setRiskLevel(resolveRiskLevel(ccInfo, total));
    }

    /**
     * 外部聯徵分數（40分）
     * 線性換算：(score - 300) / 500 * 40
     * 300 → 0分，800 → 40分
     */
    private int scoreExternalCredit(Integer externalScore) {
        if (externalScore == null) return 0;
        int clamped = Math.clamp(externalScore, EXTERNAL_SCORE_MIN, EXTERNAL_SCORE_MIN + EXTERNAL_SCORE_RANGE);
        return (int) Math.round((double) (clamped - EXTERNAL_SCORE_MIN) / EXTERNAL_SCORE_RANGE * 40);
    }

    /**
     * 收入負債比（30分）
     * 負債比 = otherBankDebt / annualIncome
     * 收入為 0 時直接給 0 分（無法評估）
     */
    private int scoreDebtRatio(BigDecimal annualIncome, BigDecimal otherBankDebt) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) == 0) return 0;
        if (otherBankDebt == null) return 30;

        BigDecimal ratio = otherBankDebt.divide(annualIncome, 4, RoundingMode.HALF_UP);
        double r = ratio.doubleValue();

        if (r < 0.3) return 30;
        if (r < 0.8) return 20;
        if (r < 1.5) return 10;
        return 0;
    }

    /**
     * 職業穩定性（15分）
     */
    private int scoreOccupation(Occupation occupation) {
        if (occupation == null) return 0;
        return switch (occupation) {
            case LEGISLATOR_MANAGER -> 15;
            case PROFESSIONAL -> 13;
            case TECHNICIAN, MILITARY, CLERICAL -> 10;
            case SERVICE_SALES, MACHINE_OPERATOR, CRAFT_WORKER -> 7;
            case AGRICULTURAL, ELEMENTARY -> 3;
            case NONE, OTHER -> 2;
        };
    }

    /**
     * 不動產（10分）
     */
    private int scoreRealEstate(Boolean hasRealEstate) {
        return Boolean.TRUE.equals(hasRealEstate) ? 10 : 0;
    }

    /**
     * finalScore → RiskLevel
     * ≥ 70 → LOW，40-69 → MEDIUM，< 40 → HIGH
     */
    private RiskLevel resolveRiskLevel(CustomerCreditInfo ccInfo, int finalScore) {

        // 優先攔截 PEP 身份
        if (Boolean.TRUE.equals(ccInfo.getIsPep())) {
            log.warn("[RiskControl] 客戶 {} 為政治敏感人物(PEP)，強制歸類為 HIGH 風險等級", ccInfo.getCustomerId());
            return RiskLevel.HIGH;
        }
        if (finalScore >= 70) return RiskLevel.LOW;
        if (finalScore >= 40) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    /**
     * 資金來源（5分）
     */
    private int scoreFundSource(FundSource fundSource) {
        if (fundSource == null) return 0;
        return switch (fundSource) {
            case SALARY -> 5;   // 薪資收入，最穩定
            case PENSION -> 4;   // 退休金，穩定
            case BUSINESS -> 3;   // 營業收入，有波動
            case SAVINGS -> 3;   // 儲蓄
            case INVESTMENT -> 2;   // 投資收益，波動大
            case INHERITANCE -> 1;   // 繼承，一次性
            case OTHER -> 1;
        };
    }

    // 2. 選填欄位覆蓋（從 RiskReviewRequest 更新 CustomerCreditInfo）
    @Transactional
    public void updateIfPresent(RiskReviewRequest dto) {
        log.info("[CreditScore] 查詢 CustomerCreditInfo customerId={}", dto.getCustomerId());
        CustomerCreditInfo info = ccRepos.findById(dto.getCustomerId())
                .orElseThrow(() -> {
                    log.error("[CreditScore] ❌ 找不到 CustomerCreditInfo，customerId={} — 請確認客戶建立時有初始化信用資料",
                            dto.getCustomerId());
                    return new IllegalArgumentException(
                            "CustomerCreditInfo not found: " + dto.getCustomerId());
                });

        log.info("[CreditScore] 找到信用資料，更新前 externalScore={}, annualIncome={}, occupation={}, otherBankDebt={}, hasRealEstate={}",
                info.getExternalScore(), info.getAnnualIncome(),
                info.getOccupation(), info.getOtherBankDebt(), info.getHasRealEstate());

        Optional.ofNullable(dto.getAnnualIncome()).ifPresent(v -> {
            log.info("[CreditScore] 覆蓋 annualIncome → {}", v);
            info.setAnnualIncome(v);
        });
        Optional.ofNullable(dto.getExternalScore()).ifPresent(v -> {
            log.info("[CreditScore] 覆蓋 externalScore → {}", v);
            info.setExternalScore(v);
        });
        Optional.ofNullable(dto.getOtherBankDebt()).ifPresent(v -> {
            log.info("[CreditScore] 覆蓋 otherBankDebt → {}", v);
            info.setOtherBankDebt(v);
        });
        Optional.ofNullable(dto.getOccupation()).ifPresent(v -> {
            log.info("[CreditScore] 覆蓋 occupation → {}", v);
            info.setOccupation(v);
        });
        Optional.ofNullable(dto.getHasRealEstate()).ifPresent(v -> {
            log.info("[CreditScore] 覆蓋 hasRealEstate → {}", v);
            info.setHasRealEstate(v);
        });

        if (dto.getAnnualIncome() == null && dto.getExternalScore() == null
                && dto.getOtherBankDebt() == null && dto.getOccupation() == null
                && dto.getHasRealEstate() == null) {
            log.info("[CreditScore] 選填欄位皆為 null，沿用資料庫既有資料（不覆蓋）");
        } else {
            // 只要有欄位被覆蓋，就必須重新計算分數與風險等級
            score(info);
            log.info("[CreditScore] 欄位覆蓋後重新評分：finalScore={}, riskLevel={}",
                    info.getFinalScore(), info.getRiskLevel());
        }

        ccRepos.save(info);
    }
}
