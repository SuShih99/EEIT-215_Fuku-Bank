package com.javaeasybank.risk.utils;

import com.javaeasybank.risk.enums.FundSource;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import com.javaeasybank.risk.enums.Occupation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.ThreadLocalRandom;

public class CreditMockUtils {

    public static CustomerCreditInfo generateMockInfo(String customerId, LocalDate birthday, Occupation occupation, BigDecimal annualIncome, FundSource fundSource, Boolean isPep, String job) {
        CustomerCreditInfo info = new CustomerCreditInfo();
        info.setCustomerId(customerId);

        // 計算年齡 (處理 null 的情況)
        int age = (birthday != null) ? Period.between(birthday, LocalDate.now()).getYears() : 30;

        // 組裝 Mock 資料
        info.setHasRealEstate(generateHasRealEstate(annualIncome, fundSource, age));
        info.setOtherBankDebt(generateDebt(annualIncome, fundSource));
        info.setExternalScore(generateExternalScore(occupation, annualIncome));

        // 根據需求塞入其他基本資料
        //info.setIsPep(isPep != null ? isPep : false);
        //info.setJobTitle(job);

        return info;
    }

    //生成有無房產
    private static boolean generateHasRealEstate(
            BigDecimal annualIncome, FundSource fundSource, int age) {

        double base = age < 30 ? 0.05 : age < 45 ? 0.20 : 0.40;

        // 高收入加成
        double incomeBonus = 0;
        if (annualIncome != null) {
            if (annualIncome.compareTo(new BigDecimal("2000000")) >= 0) incomeBonus = 0.20;
            else if (annualIncome.compareTo(new BigDecimal("1000000")) >= 0) incomeBonus = 0.10;
        }

        // 繼承或儲蓄來源，有房機率更高
        double sourceBonus = fundSource == null ? 0 : switch (fundSource) {
            case INHERITANCE -> 0.15;
            case SAVINGS -> 0.05;
            case PENSION -> 0.10;
            default -> 0;
        };

        double probability = Math.min(0.85, base + incomeBonus + sourceBonus);
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    //生成他行負債
    private static BigDecimal generateDebt(BigDecimal annualIncome, FundSource fundSource) {
        if (annualIncome == null) return BigDecimal.ZERO;

        // 資金來源決定負債傾向
        double maxDebtRatio = fundSource == null ? 1.0 : switch (fundSource) {
            case SALARY -> 0.8;   // 薪資族，負債相對低
            case PENSION -> 0.3;   // 退休金，負債低
            case BUSINESS -> 1.5;   // 營業收入，可能有較多負債
            case SAVINGS -> 0.5;
            case INVESTMENT -> 1.2;
            case INHERITANCE -> 0.2;  // 繼承，通常負債低
            case OTHER -> 1.0;
        };

        // 隨機 0 ~ maxDebtRatio 倍
        double ratio = ThreadLocalRandom.current().nextDouble(0, maxDebtRatio);
        return annualIncome.multiply(BigDecimal.valueOf(ratio))
                .setScale(2, RoundingMode.HALF_UP);
    }

    //生成外部聯徵分數
    private static int generateExternalScore(Occupation occupation, BigDecimal annualIncome) {
        // 基礎分：依職業穩定性
        int base = occupation == null ? 500 : switch (occupation) {
            case LEGISLATOR_MANAGER -> 700;
            case PROFESSIONAL -> 680;
            case TECHNICIAN, CLERICAL, MILITARY -> 640;
            case SERVICE_SALES, CRAFT_WORKER,
                 MACHINE_OPERATOR -> 580;
            case AGRICULTURAL, ELEMENTARY -> 540;
            case OTHER -> 520;
            case NONE -> 400;
        };

        // 收入加成：年收入每 100 萬加 20 分，上限 +80
        int incomeBonus = 0;
        if (annualIncome != null) {
            incomeBonus = Math.min(80,
                    annualIncome.divide(new BigDecimal("1000000"), 0, RoundingMode.DOWN)
                            .intValue() * 20);
        }

        // 加上隨機擾動 ±50，確保不同客戶有差異
        int jitter = ThreadLocalRandom.current().nextInt(-50, 51);

        return Math.clamp(base + incomeBonus + jitter, 300, 800);
    }
}

