package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.enums.BillStatus;
import com.javaeasybank.creditcard.mapper.CardBillMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardBillRepository;
import com.javaeasybank.creditcard.repository.CardTxnRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

    private final CardBillRepository cardBillRepository;
    private final CardBillMapper cardBillMapper;
    private final CardAccountRepository cardAccountRepository;
    private final CardTxnRepository cardTransactionRepository;

    public Page<CardBillResponseDto> getBills(Pageable pageable) {
        return cardBillRepository.findAll(pageable).map(cardBillMapper::toDto);
    }

    //產生帳單
    public Integer generateBills() {
        int count = 0;
        String billingMonth = YearMonth.now().toString();
        if (cardBillRepository.existsByBillingMonth(billingMonth)) {
            throw new BusinessException("Bills for this month have already been generated");
        }

        List<CardAccount> cardAccounts = cardAccountRepository.findAll();
        for (CardAccount cardAccount : cardAccounts) {
            List<CardTransaction> txns = cardTransactionRepository
                    .findByCard_CardAccount_IdAndBillIsNull(cardAccount.getId());
            if (txns.isEmpty()) {
                continue;
            }

            BigDecimal total = txns.stream()
                    .map(CardTransaction::getTxnAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CardBill bill = new CardBill();
            bill.setCardAccount(cardAccount);
            bill.setBillingMonth(billingMonth);
            bill.setBillDate(LocalDate.now());
            bill.setDueDate(LocalDate.now().plusDays(resolveDueDays(cardAccount)));
            bill.setTotalAmount(total);
            bill.setMinimumPayment(total.multiply(BigDecimal.valueOf(0.1)).setScale(0,RoundingMode.UP));
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setBillStatus(BillStatus.UNPAID);

            CardBill savedBill = cardBillRepository.save(bill);
            txns.forEach(txn -> txn.setBill(savedBill));
            cardTransactionRepository.saveAll(txns);
            count++;
        }

        return count;
    }

    public Page<CardBillResponseDto> getBillsByCustomerId(String customerId, Pageable pageable) {
        return cardBillRepository.findByCardAccountCustomerCustomerId(customerId, pageable)
                .map(cardBillMapper::toDto);
    }

    private long resolveDueDays(CardAccount cardAccount) {
        return cardAccount.getDueDays() == null ? 14L : cardAccount.getDueDays().longValue();
    }
}
