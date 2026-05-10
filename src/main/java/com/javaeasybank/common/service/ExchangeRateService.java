package com.javaeasybank.common.service;

import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String EXCHANGE_RATE_URL = "https://api.exchangerate-api.com/v4/latest/TWD";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getLatestRates() {
        try {
            Map<String, Object> response = restTemplate.getForObject(EXCHANGE_RATE_URL, Map.class);
            if (response == null || response.get("rates") == null) {
                throw new BusinessException("匯率資料格式異常");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates", e);
            throw new BusinessException("匯率獲取失敗");
        }
    }

    public BigDecimal calculateExchangeRate(Currency fromCurrency, Currency toCurrency) {
        Map<String, Object> ratesResponse = getLatestRates();
        Object ratesObject = ratesResponse.get("rates");
        if (!(ratesObject instanceof Map<?, ?> rates)) {
            throw new BusinessException("匯率資料格式異常");
        }

        BigDecimal fromRate = resolveRate(rates, fromCurrency);
        BigDecimal toRate = resolveRate(rates, toCurrency);
        return toRate.divide(fromRate, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveRate(Map<?, ?> rates, Currency currency) {
        if (currency == Currency.TWD) {
            return BigDecimal.ONE;
        }

        Object value = rates.get(currency.name());
        if (value == null) {
            throw new BusinessException("目前不支援 " + currency.name() + " 匯率");
        }
        return new BigDecimal(value.toString());
    }
}
