package com.javaeasybank.loan.utils;

/**
 * 程式說明：
 * - 本息平均攤還計算工具。
 * - 依本金、年利率與期數計算每月應繳金額、利息、本金與剩餘本金。
 */

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 本息平均攤還計算工具。
 * 依本金、年利率與期數計算每月應繳金額，並產生每一期本金/利息/剩餘本金明細。
 */
public class AmortizationCalculator {

    private AmortizationCalculator() {}

    public static BigDecimal calcMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int periods) {
        BigDecimal normalizedPrincipal = principal.setScale(0, RoundingMode.HALF_UP);
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return normalizedPrincipal.divide(BigDecimal.valueOf(periods), 0, RoundingMode.HALF_UP);
        }
        BigDecimal r            = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR     = BigDecimal.ONE.add(r);
        BigDecimal onePlusRPowN = onePlusR.pow(periods, new MathContext(20, RoundingMode.HALF_UP));
        BigDecimal numerator    = normalizedPrincipal.multiply(r).multiply(onePlusRPowN);
        BigDecimal denominator  = onePlusRPowN.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 0, RoundingMode.HALF_UP);
    }

    public static List<RepaymentRow> buildSchedule(
            BigDecimal principal,
            BigDecimal annualRate,
            int periods,
            LocalDate firstPaymentDate) {

        BigDecimal normalizedPrincipal = principal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = calcMonthlyPayment(normalizedPrincipal, annualRate, periods);
        BigDecimal r = annualRate.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        List<RepaymentRow> rows = new ArrayList<>(periods);
        BigDecimal remaining = normalizedPrincipal;

        for (int i = 1; i <= periods; i++) {
            BigDecimal interest = remaining.multiply(r).setScale(0, RoundingMode.HALF_UP);
            BigDecimal principal2;
            BigDecimal payment;

            if (i == periods) {
                principal2 = remaining;
                payment = remaining.add(interest).setScale(0, RoundingMode.HALF_UP);
            } else {
                principal2 = monthlyPayment.subtract(interest).setScale(0, RoundingMode.HALF_UP);
                payment = monthlyPayment.setScale(0, RoundingMode.HALF_UP);
            }

            remaining = remaining.subtract(principal2).setScale(0, RoundingMode.HALF_UP);

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

    public record RepaymentRow(
            int        periodIndex,
            LocalDate  scheduledDate,
            BigDecimal totalAmount,
            BigDecimal principalPortion,
            BigDecimal interestPortion,
            BigDecimal remainingAfter
    ) {}
}
