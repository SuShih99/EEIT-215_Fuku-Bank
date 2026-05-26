package com.javaeasybank.account.dto.request;

import java.math.BigDecimal;

public record AccountStats(Long count, BigDecimal sum) {
    public AccountStats(Long count, BigDecimal sum) {
        this.count = (count == null) ? 0L : count;
        this.sum = (sum == null) ? BigDecimal.ZERO : sum;
    }
}
