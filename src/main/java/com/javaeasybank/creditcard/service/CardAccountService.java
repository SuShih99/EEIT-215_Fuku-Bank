package com.javaeasybank.creditcard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardAccountResponseDto;
import com.javaeasybank.creditcard.mapper.CardAccountMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardAccountService {

    private final CardAccountRepository cardAccountRepository;
    private final CardAccountMapper cardAccountMapper;

    public CardAccountResponseDto getCardAccountById(String customerId) {
        return cardAccountRepository.findByCustomer_CustomerId(customerId)
                .map(cardAccountMapper::toDto)
                .orElseThrow(() -> new BusinessException("Card account not found with id: " + customerId));
    }

}
