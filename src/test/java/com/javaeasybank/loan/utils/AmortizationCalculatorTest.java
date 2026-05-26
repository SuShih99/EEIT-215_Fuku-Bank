package com.javaeasybank.loan.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AmortizationCalculatorTest {

    @Test
    void calcMonthlyPaymentRoundsHalfUpToInteger() {
        BigDecimal payment = AmortizationCalculator.calcMonthlyPayment(
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                3);

        assertThat(payment).isEqualByComparingTo("333");
    }

    @Test
    void buildScheduleUsesIntegerAmountsAndFinalPeriodClearsPrincipal() {
        List<AmortizationCalculator.RepaymentRow> rows = AmortizationCalculator.buildSchedule(
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                3,
                LocalDate.of(2026, 6, 1));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).totalAmount()).isEqualByComparingTo("333");
        assertThat(rows.get(1).totalAmount()).isEqualByComparingTo("333");
        assertThat(rows.get(2).totalAmount()).isEqualByComparingTo("334");
        assertThat(rows.get(2).remainingAfter()).isEqualByComparingTo("0");

        BigDecimal totalPrincipal = rows.stream()
                .map(AmortizationCalculator.RepaymentRow::principalPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPrincipal).isEqualByComparingTo("1000");
        assertThat(rows)
                .allSatisfy(row -> {
                    assertThat(row.totalAmount().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
                    assertThat(row.principalPortion().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
                    assertThat(row.interestPortion().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
                    assertThat(row.remainingAfter().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
                });
    }
}
