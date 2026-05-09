package com.javaeasybank.risk.service;

import com.javaeasybank.risk.utils.CreditMockUtils;
import com.javaeasybank.risk.core.enums.Occupation;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import com.javaeasybank.risk.repository.CustomerCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class CreditSCoreService {

    private static final int EXTERNAL_SCORE_MIN = 300;
    private static final int EXTERNAL_SCORE_RANGE = 500; // 800 - 300

    private final CustomerCreditRepository ccRepos;

    @Transactional
    public CustomerCreditInfo initializeCreditInfo(String customerId, LocalDate birthday) {
        CustomerCreditInfo info = CreditMockUtils.generateMockInfo(customerId, birthday);
        score(info);
        return ccRepos.save(info);
    }

    /**
     * 對已存在的 CustomerCreditInfo 重新評分（例如資料更新後）
     */
    @Transactional
    public CustomerCreditInfo rescore(String customerId) {
        CustomerCreditInfo info = ccRepos.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("CustomerCreditInfo not found: " + customerId));
        score(info);
        return ccRepos.save(info);
    }

    /**
     * 計算 finalScore 並設定 riskLevel，直接修改傳入的 info 物件
     */
    private void score(CustomerCreditInfo info) {
        int total = scoreExternalCredit(info.getExternalScore())
                + scoreDebtRatio(info.getAnnualIncome(), info.getOtherBankDebt())
                + scoreOccupation(info.getOccupation())
                + scoreRealEstate(info.getHasRealEstate());

        info.setFinalScore(total);
        info.setRiskLevel(resolveRiskLevel(total));
    }

    /**
     * 外部聯徵分數（40分）
     * 線性換算：(score - 300) / 500 * 40
     * 300 → 0分，800 → 40分
     */
    private int scoreExternalCredit(Integer externalScore) {
        if (externalScore == null) return 0;
        int clamped = Math.clamp(externalScore, EXTERNAL_SCORE_MIN, EXTERNAL_SCORE_MIN + EXTERNAL_SCORE_RANGE);
        return (int) Math.round((double)(clamped - EXTERNAL_SCORE_MIN) / EXTERNAL_SCORE_RANGE * 40);
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

        if (r < 0.3)  return 30;
        if (r < 0.8)  return 20;
        if (r < 1.5)  return 10;
        return 0;
    }

    /**
     * 職業穩定性（20分）
     */
    private int scoreOccupation(Occupation occupation) {
        if (occupation == null) return 0;
        return switch (occupation) {
            case GOVERNMENT_EMPLOYEE            -> 20;
            case PROFESSIONAL, MANAGER          -> 18;
            case OFFICE_WORKER, MANUFACTURING   -> 15;
            case SERVICE_INDUSTRY, SELF_EMPLOYED -> 12;
            case FREELANCER, HOUSEWIFE, RETIRED -> 8;
            case STUDENT, UNEMPLOYED            -> 3;
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
    private RiskLevel resolveRiskLevel(int finalScore) {
        if (finalScore >= 70) return RiskLevel.LOW;
        if (finalScore >= 40) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }
}
