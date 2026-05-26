package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.enums.TransactionChannel;
import com.javaeasybank.creditcard.enums.TxnType;
import com.javaeasybank.creditcard.mapper.CardTxnMapper;
import com.javaeasybank.creditcard.repository.CardTxnRepository;
import com.javaeasybank.creditcard.repository.CardTxnSpecification;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.creditcard.repository.MerchantRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardTxnService {

    private final CardTxnRepository cardTxnRepository;
    private final CreditCardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final CashbackCalculator cashbackCalculator;
    private final CardTxnMapper mapper;

    // 新增交易
    @Transactional
    public CardTxnResponseDto create(CardTxnRequestDto dto) {
        // 找信用卡
        CreditCard card = cardRepository.findById(dto.getCardId())
                .orElseThrow(() -> new BusinessException("Card not found"));
        // 檢查卡片狀態
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException("卡片尚未開通");
        }

        // 找商家
        var merchant = merchantRepository.findById(dto.getMerchantId())
                .orElseThrow(() -> new BusinessException("Merchant not found"));
        //金額不可為負數
        if (dto.getTxnAmount() == null || dto.getTxnAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("交易金額必須大於 0");
        }
        CardAccount cardAccount = card.getCardAccount();
        if (cardAccount == null || cardAccount.getCreditLimit() == null) {
            throw new BusinessException("Credit card account limit is not configured");
        }

        BigDecimal availableCredit = cardAccount.getCreditLimit()
                .subtract(calculateCurrentDebt(cardAccount));

        if (dto.getTxnAmount().compareTo(availableCredit) > 0) {
            throw new BusinessException("信用額度不足");
        }
        CardTransaction txn = new CardTransaction();

        txn.setTxnAmount(dto.getTxnAmount());

        txn.setTxnType(dto.getTxnType());

        txn.setDescription(dto.getDescription());

        txn.setTxnDate(LocalDateTime.now());

        txn.setCard(card);

        txn.setMerchant(merchant);

        txn.setChannel(
                dto.getChannel() != null
                        ? dto.getChannel()
                        : TransactionChannel.CARD);
        txn.setExternalTxnId(dto.getExternalTxnId());
        card.setCurrentDebt(zeroIfNull(card.getCurrentDebt()).add(dto.getTxnAmount()));
        // 計算回饋
        // 初始值為0，避免為null
        BigDecimal cashbackRate = BigDecimal.ZERO;

        if (card.getCardType() != null &&
                card.getCardType().getCashbackRate() != null) {

            cashbackRate = card.getCardType().getCashbackRate();
        }
        BigDecimal cashbackAmount = cashbackCalculator.calculateCashback(dto.getTxnAmount(), card);
        txn.setCashbackRate(cashbackRate);
        txn.setCashbackAmount(cashbackAmount);
        // 存檔
        CardTransaction saved = cardTxnRepository.save(txn);

        cardRepository.save(card);

        return toResponseDto(saved);
    }

    // 查全部交易(DTO)
    public Page<CardTxnResponseDto> findAll(Pageable pageable) {
        return cardTxnRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    // 查單筆交易(DTO)
    public CardTxnResponseDto findById(Integer id) {
        return toResponseDto(cardTxnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found")));
    }

    // 更新交易
    @Transactional
    public CardTxnResponseDto update(Integer id, CardTxnRequestDto dto) {
        CardTransaction txn = cardTxnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        // txn.setTxnAmount(dto.getTxnAmount());
        // txn.setTxnType(dto.getTxnType());
        txn.setDescription(dto.getDescription());

        if (dto.getCardId() != null) {
            txn.setCard(cardRepository.findById(dto.getCardId())
                    .orElseThrow(() -> new BusinessException("Card not found")));
        }

        if (dto.getMerchantId() != null) {
            txn.setMerchant(merchantRepository.findById(dto.getMerchantId())
                    .orElseThrow(() -> new BusinessException("Merchant not found")));
        }

        CardTransaction saved = cardTxnRepository.save(txn);
        return toResponseDto(saved);
    }

    // 刪除交易(不可刪除交易)
    @Transactional
    public void deleteById(Integer id) {
        if (!cardTxnRepository.existsById(id)) {
            throw new BusinessException("Transaction not found");
        }
        cardTxnRepository.deleteById(id);
    }

    // 刷退交易
    public CardTxnResponseDto refund(Integer id) {
        // 找原交易
        CardTransaction originalTxn = cardTxnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        // 防止重複刷退
        if (originalTxn.getTxnType() == TxnType.REFUND) {
            throw new BusinessException("此交易已為刷退交易");
        }
        if (cardTxnRepository.existsByRefTxn_TxnId(id)) {
            throw new BusinessException("此交易已刷退");
        }

        // 建立刷退交易
        CardTransaction refundTxn = new CardTransaction();
        refundTxn.setCard(originalTxn.getCard());

        refundTxn.setMerchant(originalTxn.getMerchant());

        refundTxn.setTxnDate(LocalDateTime.now());
        refundTxn.setTxnAmount(
                originalTxn.getTxnAmount().negate());
        refundTxn.setTxnType(TxnType.REFUND);
        refundTxn.setDescription(
                "Refund - " + originalTxn.getDescription());
        // refundTxn.setOriginalTxn(originalTxn);
        refundTxn.setRefTxn(originalTxn);
        CreditCard card = originalTxn.getCard();
        card.setCurrentDebt(zeroIfNull(card.getCurrentDebt()).subtract(originalTxn.getTxnAmount()));
        // 計算回饋
        refundTxn.setCashbackRate(originalTxn.getCashbackRate());
        if (originalTxn.getCashbackAmount() != null) {
            refundTxn.setCashbackAmount(originalTxn.getCashbackAmount().negate());
        } else {
            refundTxn.setCashbackAmount(BigDecimal.ZERO);
        }

        // 存入資料庫
        CardTransaction savedTxn = cardTxnRepository.save(refundTxn);
        cardRepository.save(card);

        return toResponseDto(savedTxn);
    }

    private CardTxnResponseDto toResponseDto(CardTransaction txn) {
        CardTxnResponseDto dto = mapper.toDto(txn);
        applyCashbackFallback(txn, dto);
        dto.setRefunded(
                txn.getTxnType() != TxnType.REFUND
                        && cardTxnRepository.existsByRefTxn_TxnId(txn.getTxnId()));
        return dto;
    }

    private void applyCashbackFallback(CardTransaction txn, CardTxnResponseDto dto) {
        if (txn.getTxnType() != TxnType.PURCHASE && txn.getTxnType() != TxnType.REFUND) {
            return;
        }

        BigDecimal cashbackRate = resolveCashbackRate(txn);
        if (dto.getCashbackRate() == null) {
            dto.setCashbackRate(cashbackRate);
        }

        if (shouldCalculateCashback(txn, dto)) {
            dto.setCashbackAmount(cashbackCalculator.calculateCashback(txn.getTxnAmount(), cashbackRate));
        }
    }

    private boolean shouldCalculateCashback(CardTransaction txn, CardTxnResponseDto dto) {
        if (dto.getCashbackAmount() == null) {
            return true;
        }

        return txn.getTxnType() == TxnType.REFUND
                && BigDecimal.ZERO.compareTo(dto.getCashbackAmount()) == 0
                && txn.getRefTxn() != null
                && txn.getRefTxn().getCashbackAmount() == null;
    }

    private BigDecimal resolveCashbackRate(CardTransaction txn) {
        if (txn.getCashbackRate() != null) {
            return txn.getCashbackRate();
        }
        if (txn.getRefTxn() != null && txn.getRefTxn().getCashbackRate() != null) {
            return txn.getRefTxn().getCashbackRate();
        }
        if (txn.getCard() != null
                && txn.getCard().getCardType() != null
                && txn.getCard().getCardType().getCashbackRate() != null) {
            return txn.getCard().getCardType().getCashbackRate();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCurrentDebt(CardAccount cardAccount) {
        if (cardAccount.getCards() == null) {
            return BigDecimal.ZERO;
        }

        return cardAccount.getCards().stream()
                .map(CreditCard::getCurrentDebt)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public Page<CardTxnResponseDto> findByCustomerId(String customerId, Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "txnDate"));

        return cardTxnRepository.findByCard_Customer_CustomerId(
                customerId,
                sortedPageable).map(mapper::toDto);
    }

    public Page<CardTxnResponseDto> getUnbilledBillsByCustomerId(String customerId, Pageable pageable) {
        return cardTxnRepository.findByCard_Customer_CustomerIdAndBillIsNull(customerId, pageable)
                .map(mapper::toDto);
    }

    public Page<CardTxnResponseDto> search(String keyword,
            TxnType txnType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        Specification<CardTransaction> spec = CardTxnSpecification.search(keyword, txnType, startDate, endDate);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "txnDate"));

        return cardTxnRepository.findAll(spec, sortedPageable)
                .map(this::toResponseDto);
    }

    public List<CardTxnResponseDto> getBilledTransactions(
            String customerId,
            Integer billId,
            Integer cardId) {

        List<CardTransaction> txns = cardTxnRepository
                .findByBill_BillIdAndCard_CardIdAndCard_CardAccount_Customer_CustomerIdOrderByTxnDateDesc(
                        billId,
                        cardId,
                        customerId);

        return txns.stream()
                .map(mapper::toDto)
                .toList();
    }

}
