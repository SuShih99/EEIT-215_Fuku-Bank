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

import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.response.CashResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.account.service.TransferService;
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
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final CardBillRepository cardBillRepository;
    private final CardBillMapper cardBillMapper;
    private final CardAccountRepository cardAccountRepository;
    private final CardTxnRepository cardTransactionRepository;
    private final TransferService transferService;
    private final AccountIntegrationService accountIntegrationService;

    // 查帳單

    public Page<CardBillResponseDto> getBills(Pageable pageable) {
        return cardBillRepository.findAll(pageable).map(cardBillMapper::toDto);
    }

    // 產生帳單
    public Integer generateBills() {
        int count = 0;
        String billingMonth = YearMonth.now().toString();

        // if (cardBillRepository.existsByBillingMonth(billingMonth)) {
        // throw new BusinessException("Bills for this month have already been
        // generated");
        // }

        List<CardAccount> cardAccounts = cardAccountRepository.findAll();
        for (CardAccount cardAccount : cardAccounts) {

            // 已存在本月帳單則跳過
            if (cardBillRepository.existsByCardAccountIdAndBillingMonth(cardAccount.getId(), billingMonth)) {
                continue;
            }

            List<CardTransaction> txns = cardTransactionRepository
                    .findByCard_CardAccount_IdAndBillIsNull(cardAccount.getId());
            if (txns.isEmpty()) {
                continue;
            }

            BigDecimal total = txns.stream()
                    .map(CardTransaction::getTxnAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cashbackAmount = txns.stream()
                    .map(CardTransaction::getCashbackAmount)
                    .filter(cashback -> cashback != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CardBill bill = new CardBill();
            bill.setCardAccount(cardAccount);
            bill.setBillingMonth(billingMonth);
            bill.setBillDate(LocalDate.now());
            bill.setDueDate(LocalDate.now().plusDays(resolveDueDays(cardAccount)));
            bill.setTotalAmount(total);
            BigDecimal minimumPayment = total.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO
                    : total.multiply(BigDecimal.valueOf(0.1)).setScale(0, RoundingMode.UP);

            bill.setMinimumPayment(minimumPayment);
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setBillStatus(BillStatus.UNPAID);

            bill.setCashbackAmount(cashbackAmount);
            bill.setRewardPosted(false);

            CardBill savedBill = cardBillRepository.save(bill);

            postCashbackReward(savedBill, cardAccount, cashbackAmount, billingMonth);

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

    private void postCashbackReward(
            CardBill savedBill,
            CardAccount cardAccount,
            BigDecimal cashbackAmount,
            String billingMonth) {
        if (cashbackAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        try {
            String customerId = cardAccount.getCustomer().getCustomerId();

            AccountResponse rewardAccount = accountIntegrationService
                    .getActiveTwdCheckingAccounts(customerId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("找不到可入帳的台幣活存帳戶"));

            CashRequest rewardRequest = new CashRequest();
            rewardRequest.setAccountNumber(rewardAccount.getAccountNumber());
            rewardRequest.setAmount(cashbackAmount);
            rewardRequest.setNote("CARD_REWARD " + billingMonth);

            CashResponse rewardResponse = transferService.creditCardReward(rewardRequest);

            savedBill.setRewardReferenceId(rewardResponse.getReferenceId());
            savedBill.setRewardPosted(true);
            cardBillRepository.save(savedBill);
        } catch (BusinessException e) {
            savedBill.setRewardPosted(false);
            cardBillRepository.save(savedBill);
            log.warn(
                    "Failed to post card reward. billId={}, cardAccountId={}, billingMonth={}, amount={}, reason={}",
                    savedBill.getBillId(),
                    cardAccount.getId(),
                    billingMonth,
                    cashbackAmount,
                    e.getMessage());
        }
    }
}
