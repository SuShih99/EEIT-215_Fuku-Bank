package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.TxnType;
import com.javaeasybank.creditcard.mapper.CardTxnMapper;
import com.javaeasybank.creditcard.repository.CardTxnRepository;
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
    private final CardTxnMapper mapper;

    // 新增交易
    @Transactional
    public CardTxnResponseDto create(CardTxnRequestDto dto) {
        // 找信用卡
    CreditCard card = cardRepository.findById(dto.getCardId())
            .orElseThrow(() -> new BusinessException("Card not found"));

    // 找商家
    var merchant = merchantRepository.findById(dto.getMerchantId())
            .orElseThrow(() -> new BusinessException("Merchant not found"));

    // ===== 額度檢查 =====
    BigDecimal availableCredit = card.getCreditLimit()
            .subtract(card.getCurrentBalance());

    if (dto.getTxnAmount().compareTo(availableCredit) > 0) {
        throw new BusinessException("信用額度不足");
    }

    // ===== 建立交易 =====
    CardTransaction txn = new CardTransaction();

    txn.setTxnAmount(dto.getTxnAmount());

    txn.setTxnType(dto.getTxnType());

    txn.setDescription(dto.getDescription());

    txn.setTxnDate(LocalDateTime.now());

    txn.setCard(card);

    txn.setMerchant(merchant);

    // ===== 更新已使用額度 =====
    card.setCurrentBalance(
            card.getCurrentBalance()
                    .add(dto.getTxnAmount())
    );

    // 存檔
    CardTransaction saved = cardTxnRepository.save(txn);

    cardRepository.save(card);

    return mapper.toDto(saved);
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

        txn.setTxnAmount(dto.getTxnAmount());
        txn.setTxnType(dto.getTxnType());
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
        return mapper.toDto(saved);
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

        // ===== 基本資料 =====
        refundTxn.setCard(originalTxn.getCard());

        refundTxn.setMerchant(originalTxn.getMerchant());

        refundTxn.setTxnDate(LocalDateTime.now());

        // ===== 刷退金額（負數）=====
        refundTxn.setTxnAmount(
                originalTxn.getTxnAmount().negate());

        // ===== 類型 =====
        refundTxn.setTxnType(TxnType.REFUND);

        // ===== 描述 =====
        refundTxn.setDescription(
                "Refund - " + originalTxn.getDescription());

        // ===== 可選：紀錄原交易 =====
        // refundTxn.setOriginalTxn(originalTxn);
        refundTxn.setRefTxn(originalTxn);

        // ===== 更新信用卡已使用額度 =====
        CreditCard card = originalTxn.getCard();
        card.setCurrentBalance(
                card.getCurrentBalance()
                        .subtract(originalTxn.getTxnAmount()));

        // 存入資料庫
        CardTransaction savedTxn = cardTxnRepository.save(refundTxn);
        cardRepository.save(card);

        return toResponseDto(savedTxn);
    }

    private CardTxnResponseDto toResponseDto(CardTransaction txn) {
        CardTxnResponseDto dto = mapper.toDto(txn);
        dto.setRefunded(
                txn.getTxnType() != TxnType.REFUND
                        && cardTxnRepository.existsByRefTxn_TxnId(txn.getTxnId()));
        return dto;
    }
}
