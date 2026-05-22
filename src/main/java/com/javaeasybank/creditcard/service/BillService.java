package com.javaeasybank.creditcard.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.response.CashResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.account.service.TransferService;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.enums.BillStatus;
import com.javaeasybank.creditcard.mapper.CardBillMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardBillRepository;
import com.javaeasybank.creditcard.repository.CardBillSpecification;
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
    private final EmailService emailService;
    private final CardBillPDFService cardBillPDFService;
    private final NotificationService notificationService;

    // 查帳單

    public Page<CardBillResponseDto> getBills(String customerName,
            String billingMonth,
            BillStatus billStatus,
            Pageable pageable) {

        Specification<CardBill> spec = CardBillSpecification.search(customerName, billingMonth, billStatus);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("billingMonth"),
                        Sort.Order.desc("billId")));
        return cardBillRepository.findAll(spec, sortedPageable).map(cardBillMapper::toDto);
    }

    // 產生帳單
    public Integer generateBills() throws IOException {
        int count = 0;
        int skippedExists = 0;
        int skippedNoTxns = 0;
        int processed = 0;
        String billingMonth = YearMonth.now().toString();

        // if (cardBillRepository.existsByBillingMonth(billingMonth)) {
        // throw new BusinessException("Bills for this month have already been
        // generated");
        // }

        int page = 0;
        int size = 20;
        Page<CardAccount> cardAccountPage;
        do {
            cardAccountPage = cardAccountRepository.findAll(
                    PageRequest.of(page, size, Sort.by("id").ascending()));

            for (CardAccount cardAccount : cardAccountPage.getContent()) {
                processed++;

                // 已存在本月帳單則跳過
                if (cardBillRepository.existsByCardAccountIdAndBillingMonth(cardAccount.getId(), billingMonth)) {
                    skippedExists++;
                    continue;
                }

                List<CardTransaction> txns = cardTransactionRepository
                        .findByCard_CardAccount_IdAndBillIsNull(cardAccount.getId());

                List<CardBill> previousBills = cardBillRepository
                        .findByCardAccountIdAndBillStatusInOrderByBillingMonthAsc(
                                cardAccount.getId(),
                                List.of(BillStatus.UNPAID, BillStatus.PARTIAL));

                BigDecimal previousUnpaid = previousBills.stream()
                        .map(oldBill -> oldBill.getTotalAmount().subtract(oldBill.getPaidAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal currentTotal = txns.stream()
                        .map(CardTransaction::getTxnAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal total = previousUnpaid.add(currentTotal);

                // 沒有任何應繳金額才跳過
                if (total.compareTo(BigDecimal.ZERO) <= 0) {
                    skippedNoTxns++;
                    continue;
                }

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

                if (previousUnpaid.compareTo(BigDecimal.ZERO) > 0) {
                    previousBills.forEach(oldBill -> oldBill.setBillStatus(BillStatus.ROLLED_OVER));
                    cardBillRepository.saveAll(previousBills);
                }

                /*
                 * 寄文字格式
                 * emailService.sendCardBillStatementEmail(
                 * cardAccount.getCustomer().getEmail(),
                 * cardAccount.getCustomer().getName(),
                 * billingMonth,
                 * total,
                 * minimumPayment,
                 * savedBill.getDueDate(),
                 * savedBill.getBillId());
                 */
                String idNumber = cardAccount.getCustomer().getIdNumber();
                String pdfPassword = getIdNumberLast4(idNumber);

                byte[] pdfBytes = cardBillPDFService.generateBillPdf(
                        cardAccount.getCustomer().getName(),
                        billingMonth,
                        total,
                        minimumPayment,
                        savedBill.getDueDate(),
                        pdfPassword);

                emailService.sendEmailWithAttachment(
                        cardAccount.getCustomer().getEmail(),
                        "Java Easy Bank - 信用卡月結帳單",
                        "您的信用卡月結帳單已產生，PDF 附件請使用身分證字號末 4 碼開啟。",
                        "card-bill-" + billingMonth + ".pdf",
                        pdfBytes);
                notificationService.createNotification(
                        cardAccount.getCustomer().getCustomerId(),
                        NotificationType.CREDIT_CARD,
                        "信用卡帳單已寄出",
                        "您的本期信用卡月結帳單已產生，請至信箱查看附件。",
                        "/user/card-bills");

                // 寄檔案到本地
                /*
                 * CustomerProfile customer = cardAccount.getCustomer();
                 * String last4 = customer.getIdNumber()
                 * .substring(customer.getIdNumber().length() - 4);
                 * Files.write(
                 * Paths.get(
                 * "uploads",
                 * customer.getName()
                 * + "-"
                 * + last4
                 * + ".pdf"),
                 * pdfBytes);
                 */

                postCashbackReward(savedBill, cardAccount, cashbackAmount, billingMonth);

                txns.forEach(txn -> txn.setBill(savedBill));
                cardTransactionRepository.saveAll(txns);
                count++;
            }

            page++;

        } while (cardAccountPage.hasNext());

        log.info("帳單產生完成 processed={}, generated={}, skippedExists={}, skippedNoTxns={}",
                processed, count, skippedExists, skippedNoTxns);
        return count;
    }

    public Page<CardBillResponseDto> getBillsByCustomerId(String customerId, Pageable pageable) {
        return cardBillRepository.findByCardAccountCustomerCustomerId(customerId, pageable)
                .map(cardBillMapper::toDto);
    }

    private long resolveDueDays(CardAccount cardAccount) {
        return cardAccount.getDueDays() == null ? 14L : cardAccount.getDueDays().longValue();
    }

    private String getIdNumberLast4(String idNumber) {
        if (idNumber == null || idNumber.length() < 4) {
            throw new BusinessException("客戶身分證字號資料不完整，無法產生 PDF 密碼");
        }

        return idNumber.substring(idNumber.length() - 4);
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
