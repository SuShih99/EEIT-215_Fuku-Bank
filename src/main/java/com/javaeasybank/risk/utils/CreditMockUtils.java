package com.javaeasybank.risk.utils;

import com.javaeasybank.risk.core.enums.Occupation;
import com.javaeasybank.risk.entity.CustomerCreditInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.ThreadLocalRandom;

public class CreditMockUtils {

    // 各年齡段可選的職業池
    private static final Occupation[] YOUNG_OCCUPATIONS = {
            Occupation.STUDENT,
            Occupation.OFFICE_WORKER,
            Occupation.MANUFACTURING,
            Occupation.SERVICE_INDUSTRY,
            Occupation.FREELANCER,
            Occupation.GOVERNMENT_EMPLOYEE
    };

    private static final Occupation[] MID_OCCUPATIONS = {
            Occupation.OFFICE_WORKER,
            Occupation.GOVERNMENT_EMPLOYEE,
            Occupation.MANAGER,
            Occupation.PROFESSIONAL,
            Occupation.MANUFACTURING,
            Occupation.SERVICE_INDUSTRY,
            Occupation.SELF_EMPLOYED,
            Occupation.FREELANCER,
            Occupation.HOUSEWIFE
    };

    private static final Occupation[] SENIOR_OCCUPATIONS = {
            Occupation.MANAGER,
            Occupation.PROFESSIONAL,
            Occupation.GOVERNMENT_EMPLOYEE,
            Occupation.SELF_EMPLOYED,
            Occupation.SERVICE_INDUSTRY,
            Occupation.RETIRED,
            Occupation.HOUSEWIFE,
            Occupation.UNEMPLOYED
    };

    /**
     * 根據 customerId 與生日，產生一筆模擬信用資料
     *
     * @param customerId 客戶ID，必須與 CustomerProfile 一致
     * @param birthday   客戶生日，用於推算年齡與職業分布
     * @return 尚未持久化的 CustomerCreditInfo
     * @throws IllegalArgumentException 若生日為未來日期
     */
    public static CustomerCreditInfo generateMockInfo(String customerId, LocalDate birthday) {
        validateBirthday(birthday);

        int age = Period.between(birthday, LocalDate.now()).getYears();
        Occupation occupation = selectOccupationByAge(age);

        CustomerCreditInfo info = new CustomerCreditInfo();
        info.setCustomerId(customerId);
        info.setOccupation(occupation);
        info.setAnnualIncome(generateIncome(occupation, age));
        info.setOtherBankDebt(generateDebt(info.getAnnualIncome()));
        info.setExternalScore(ThreadLocalRandom.current().nextInt(300, 801));
        info.setHasRealEstate(generateHasRealEstate(age));

        return info;
    }

    private static void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday must not be null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future: " + birthday);
        }
    }

    private static Occupation selectOccupationByAge(int age) {
        var random = ThreadLocalRandom.current();

        if (age < 22) return Occupation.STUDENT;

        Occupation[] pool = age < 35 ? YOUNG_OCCUPATIONS
                : age < 55 ? MID_OCCUPATIONS
                  : SENIOR_OCCUPATIONS;

        return pool[random.nextInt(pool.length)];
    }

    /**
     * 年齡加成邏輯：
     * 每多一歲，在基礎收入上加 seniority * 年資乘數
     * 年資從各職業的「預設起始年齡」開始計算
     */
    private static BigDecimal generateIncome(Occupation occupation, int age) {
        var random = ThreadLocalRandom.current();

        record Income(double base, double bonus) {}

        Income inc = switch (occupation) {
            case OFFICE_WORKER ->
                    new Income(400_000 + random.nextInt(400_000),
                            Math.max(0, age - 25) * 8_000.0);
            case GOVERNMENT_EMPLOYEE ->
                    new Income(500_000 + random.nextInt(200_000),
                            Math.max(0, age - 25) * 6_000.0);
            case PROFESSIONAL ->
                    new Income(1_500_000 + random.nextInt(500_000),
                            Math.max(0, age - 30) * 30_000.0);
            case MANAGER ->
                    new Income(1_000_000 + random.nextInt(500_000),
                            Math.max(0, age - 35) * 20_000.0);
            case MANUFACTURING ->
                    new Income(350_000 + random.nextInt(150_000),
                            Math.max(0, age - 22) * 3_000.0);
            case SERVICE_INDUSTRY ->
                    new Income(300_000 + random.nextInt(200_000),
                            Math.max(0, age - 22) * 2_000.0);
            case SELF_EMPLOYED ->
                    new Income(500_000 + random.nextInt(1_000_000),
                            Math.max(0, age - 30) * 15_000.0);
            case FREELANCER ->
                    new Income(400_000 + random.nextInt(800_000),
                            Math.max(0, age - 28) * 10_000.0);
            case STUDENT ->
                    new Income(50_000 + random.nextInt(100_000), 0);
            case RETIRED ->
                    new Income(200_000 + random.nextInt(200_000), 0);
            case HOUSEWIFE ->
                    new Income(random.nextInt(100_000), 0);
            case UNEMPLOYED ->
                    new Income(random.nextInt(50_000), 0);
        };

        return BigDecimal.valueOf(inc.base() + inc.bonus())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 負債為年收入的 0.0 ~ 2.0 倍隨機值
     * 上限 2 倍模擬最壞情況（房貸 + 信貸並存）
     */
    private static BigDecimal generateDebt(BigDecimal annualIncome) {
        // 先產生整數倍數避免 double 精度問題：0 ~ 200 代表 0.00 ~ 2.00
        int debtPermille = ThreadLocalRandom.current().nextInt(0, 201); // 0~200
        BigDecimal factor = BigDecimal.valueOf(debtPermille, 2);        // scale=2，即除以100
        return annualIncome.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 有房機率隨年齡提升：
     * < 30 歲：5%，30~45 歲：20%，> 45 歲：40%
     */
    private static boolean generateHasRealEstate(int age) {
        double probability = age < 30 ? 0.05
                : age < 45 ? 0.20
                  : 0.40;
        return ThreadLocalRandom.current().nextDouble() < probability;
    }
}

