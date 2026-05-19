package com.javaeasybank.loan.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 等額本息（本息平均攤還）計算工具。
 *
 * <p>純靜態工具類，無 Spring 依賴，可直接進行單元測試。
 * 提供兩個公開入口：</p>
 * <ul>
 *   <li>{@link #calcMonthlyPayment} — 計算每月應繳固定金額（元，無條件進位）</li>
 *   <li>{@link #buildSchedule} — 展開完整攤還表，逐期列出本金 / 利息 / 剩餘本金</li>
 * </ul>
 *
 * <p><b>計算公式（等額本息月付金）：</b></p>
 * <pre>
 * M = P × r × (1+r)^n / ((1+r)^n − 1)
 *
 * P = 撥款本金
 * r = 月利率（年利率 ÷ 12）
 * n = 還款總期數（月）
 * </pre>
 * <p>若年利率為 0，退化為平均攤還：{@code M = ⌈P / n⌉}。</p>
 */
public class AmortizationCalculator {

    /** 工具類，禁止外部實例化。 */
    private AmortizationCalculator() {}

    /**
     * 計算每月應繳的固定金額（等額本息月付金）。
     *
     * <p>結果使用 {@code RoundingMode.CEILING}（無條件進位），
     * 確保每期金額不低於理論值，避免最終期因累積誤差而短收。</p>
     *
     * @param principal  撥款本金（新台幣，需大於零）
     * @param annualRate 年利率，例如 {@code 0.05} 代表 5%；傳入 {@code 0} 時退化為平均攤還
     * @param periods    還款總期數（月，需大於零）
     * @return 每月應繳金額（無條件進位至整數元）
     */
    public static BigDecimal calcMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int periods) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            // 零利率：直接平均攤還，無條件進位
            return principal.divide(BigDecimal.valueOf(periods), 0, RoundingMode.CEILING);
        }
        BigDecimal r            = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR     = BigDecimal.ONE.add(r);
        BigDecimal onePlusRPowN = onePlusR.pow(periods, new MathContext(20, RoundingMode.HALF_UP));
        BigDecimal numerator    = principal.multiply(r).multiply(onePlusRPowN);
        BigDecimal denominator  = onePlusRPowN.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 0, RoundingMode.CEILING);
    }

    /**
     * 展開完整攤還表，逐期計算本金 / 利息 / 繳完後剩餘本金。
     *
     * <p>最後一期直接清零剩餘本金，以消除前面各期四捨五入所累積的誤差，
     * 確保所有期數繳完後本金餘額恰好為 0。</p>
     *
     * @param principal        撥款本金
     * @param annualRate       年利率，例如 {@code 0.04} 代表 4%
     * @param periods          還款總期數（月）
     * @param firstPaymentDate 第 1 期應繳日（通常為 {@code startDate} 加 1 個月）
     * @return 長度為 {@code periods} 的攤還明細清單，索引 0 對應第 1 期
     */
    public static List<RepaymentRow> buildSchedule(
            BigDecimal principal,
            BigDecimal annualRate,
            int periods,
            LocalDate firstPaymentDate) {

        BigDecimal monthlyPayment = calcMonthlyPayment(principal, annualRate, periods);
        BigDecimal r = annualRate.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        List<RepaymentRow> rows = new ArrayList<>(periods);
        BigDecimal remaining = principal;

        for (int i = 1; i <= periods; i++) {
            // 本期利息 = 剩餘本金 × 月利率（四捨五入至整數元）
            BigDecimal interest = remaining.multiply(r).setScale(0, RoundingMode.HALF_UP);
            BigDecimal principal2;
            BigDecimal payment;

            if (i == periods) {
                // 最後一期：直接清零，消除累積舍入誤差
                principal2 = remaining;
                payment    = remaining.add(interest);
            } else {
                principal2 = monthlyPayment.subtract(interest);
                payment    = monthlyPayment;
            }

            remaining = remaining.subtract(principal2);

            rows.add(new RepaymentRow(
                    i,
                    firstPaymentDate.plusMonths(i - 1),
                    payment,
                    principal2,
                    interest,
                    remaining
            ));
        }
        return rows;
    }

    /**
     * 單期攤還明細（不可變 record）。
     *
     * @param periodIndex      期數（1-based）
     * @param scheduledDate    本期預計應繳日
     * @param totalAmount      本期應繳總額（本金 + 利息）
     * @param principalPortion 本期還款中的本金部分
     * @param interestPortion  本期還款中的利息部分
     * @param remainingAfter   繳清本期後的剩餘貸款本金
     */
    public record RepaymentRow(
            int        periodIndex,
            LocalDate  scheduledDate,
            BigDecimal totalAmount,
            BigDecimal principalPortion,
            BigDecimal interestPortion,
            BigDecimal remainingAfter
    ) {}
}
