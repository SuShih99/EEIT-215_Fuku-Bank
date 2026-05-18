package com.javaeasybank.risk.utils;

import com.javaeasybank.account.enums.FundSource;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import com.javaeasybank.risk.enums.Occupation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 信用資料模擬工具
 * 只負責填入開戶蒐集不到的欄位：
 *   - externalScore（聯徵分數）
 *   - otherBankDebt（他行負債）
 *   - hasRealEstate（有無不動產）
 *
 * occupation / annualIncome / fundSource / isPep / industry
 * 已由開戶真實資料填入，不在此產生。
 */
public class CreditMockUtils {

    private CreditMockUtils() {}

    /**
     * 填入開戶蒐集不到的欄位，使用真實資料推算，加入隨機擾動增加差異性。
     *
     * @param info     已填入真實職業、收入、資金來源的 CustomerCreditInfo
     * @param birthday 客戶生日（用於年齡推算不動產機率）
     */
    public static void fillMissingFields(CustomerCreditInfo info, LocalDate birthday) {
        validateBirthday(birthday);
        int age = Period.between(birthday, LocalDate.now()).getYears();

        info.setExternalScore(
                generateExternalScore(info.getOccupation(), info.getAnnualIncome()));

        info.setOtherBankDebt(
                generateDebt(info.getAnnualIncome(), info.getFundSource()));

        info.setHasRealEstate(
                generateHasRealEstate(info.getAnnualIncome(), info.getFundSource(), age));
    }

    // ── 聯徵分數 ──────────────────────────────────────────────────────────────

    /**
     * 依職業穩定性為基礎，年收入加成，加上隨機擾動。
     * 範圍：300 ~ 800
     */
    private static int generateExternalScore(Occupation occupation, BigDecimal annualIncome) {

        int base = occupation == null ? 500 : switch (occupation) {
            case LEGISLATOR_MANAGER              -> 700;
            case PROFESSIONAL                    -> 680;
            case TECHNICIAN, CLERICAL, MILITARY  -> 640;
            case SERVICE_SALES, CRAFT_WORKER,
                 MACHINE_OPERATOR               -> 580;
            case AGRICULTURAL, ELEMENTARY        -> 540;
            case OTHER                           -> 520;
            case NONE                            -> 400;
        };

        // 年收入每 100 萬加 20 分，上限 +80
        int incomeBonus = 0;
        if (annualIncome != null && annualIncome.compareTo(BigDecimal.ZERO) > 0) {
            incomeBonus = Math.min(80,
                    annualIncome.divide(new BigDecimal("1000000"), 0, RoundingMode.DOWN)
                            .intValue() * 20);
        }

        // 隨機擾動 ±50
        int jitter = ThreadLocalRandom.current().nextInt(-50, 51);

        return Math.clamp(base + incomeBonus + jitter, 300, 800);
    }

    // ── 他行負債 ──────────────────────────────────────────────────────────────

    /**
     * 依資金來源決定負債傾向，隨機生成年收入的 0 ~ maxRatio 倍。
     */
    private static BigDecimal generateDebt(BigDecimal annualIncome, FundSource fundSource) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        double maxRatio = fundSource == null ? 1.0 : switch (fundSource) {
            case SALARY      -> 0.8;
            case RETIREMENT     -> 0.3;
            case BUSINESS_INCOME    -> 1.5;
            case SAVINGS     -> 0.5;
            case INVESTMENT  -> 1.2;
            case INHERITANCE -> 0.2;
            case OTHER       -> 1.0;
        };

        double ratio = ThreadLocalRandom.current().nextDouble(0, maxRatio);
        return annualIncome.multiply(BigDecimal.valueOf(ratio))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── 有無不動產 ────────────────────────────────────────────────────────────

    /**
     * 依年齡為基礎機率，高收入與特定資金來源加成。
     * 上限 85%。
     */
    private static boolean generateHasRealEstate(
            BigDecimal annualIncome, FundSource fundSource, int age) {

        double base = age < 30 ? 0.05
                : age < 45 ? 0.20
                  : 0.40;

        double incomeBonus = 0;
        if (annualIncome != null) {
            if (annualIncome.compareTo(new BigDecimal("2000000")) >= 0)      incomeBonus = 0.20;
            else if (annualIncome.compareTo(new BigDecimal("1000000")) >= 0) incomeBonus = 0.10;
        }

        double sourceBonus = fundSource == null ? 0 : switch (fundSource) {
            case INHERITANCE -> 0.15;
            case RETIREMENT     -> 0.10;
            case SAVINGS     -> 0.05;
            default          -> 0;
        };

        double probability = Math.min(0.85, base + incomeBonus + sourceBonus);
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    // ── 驗證 ──────────────────────────────────────────────────────────────────

    private static void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday must not be null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future: " + birthday);
        }
    }
}
