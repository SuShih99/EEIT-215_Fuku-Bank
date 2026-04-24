package com.javaeasybank.creditcard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.entity.CardTransaction;
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
	
	public CardTxnResponseDto create(CardTxnRequestDto dto) {

        CardTransaction txn = new CardTransaction();
        txn.setTxnAmount(dto.getTxnAmount());
        txn.setTxnType(dto.getTxnType());
        txn.setDescription(dto.getDescription());

        txn.setCard(cardRepository.findById(dto.getCardId())
                .orElseThrow(() -> new BusinessException("Card not found")));

        txn.setMerchant(merchantRepository.findById(dto.getMerchantId())
                .orElseThrow(() -> new BusinessException("Merchant not found")));

        CardTransaction saved = cardTxnRepository.save(txn);

        return toDto(saved);
    }
	private CardTxnResponseDto toDto(CardTransaction txn) {
        CardTxnResponseDto dto = new CardTxnResponseDto();
        dto.setTxnId(txn.getTxnId());
        dto.setTxnAmount(txn.getTxnAmount());
        dto.setTxnType(txn.getTxnType());
        dto.setTxnDate(txn.getTxnDate());
        dto.setDescription(txn.getDescription());

        dto.setMerchantName(txn.getMerchant().getMerchantName());
        dto.setCardNumber(maskCard(txn.getCard().getCardNumber()));

        return dto;
    }

    private String maskCard(String cardNumber) {
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
	
}
