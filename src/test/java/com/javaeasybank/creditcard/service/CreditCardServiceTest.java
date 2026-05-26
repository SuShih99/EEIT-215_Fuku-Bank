package com.javaeasybank.creditcard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.entity.CardType;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.enums.TxnType;
import com.javaeasybank.creditcard.mapper.CreditCardMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardTxnRepository;
import com.javaeasybank.creditcard.repository.CardTypeRepository;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.customer.repository.CustomerProfileRepository;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository cardRepository;

    @Mock
    private CardTypeRepository cardTypeRepository;

    @Mock
    private CardAppItemRepository itemRepository;

    @Mock
    private CreditCardMapper mapper;

    @Mock
    private AccountIntegrationService accountIntegrationService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardAccountRepository cardAccountRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CardTxnRepository cardTxnRepository;

    @InjectMocks
    private CreditCardService service;

    @Test
    @DisplayName("activeCard creates one unbilled annual fee transaction")
    void activeCard_createsAnnualFeeTransaction() {
        CreditCard card = inactiveCardWithAnnualFee(new BigDecimal("1200.00"));
        CreditCardResponseDto response = new CreditCardResponseDto();
        response.setCardId(10);
        response.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(10)).thenReturn(Optional.of(card));
        when(cardTxnRepository.existsByCard_CardIdAndTxnType(10, TxnType.ANNUAL_FEE)).thenReturn(false);
        when(cardRepository.save(card)).thenReturn(card);
        when(mapper.toDto(card)).thenReturn(response);

        service.activeCard(10);

        ArgumentCaptor<CardTransaction> txnCaptor = ArgumentCaptor.forClass(CardTransaction.class);
        verify(cardTxnRepository).save(txnCaptor.capture());

        CardTransaction txn = txnCaptor.getValue();
        assertEquals(TxnType.ANNUAL_FEE, txn.getTxnType());
        assertEquals(0, new BigDecimal("1200.00").compareTo(txn.getTxnAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(txn.getCashbackRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(txn.getCashbackAmount()));
        assertNull(txn.getBill());
        assertEquals(0, new BigDecimal("1200.00").compareTo(card.getCurrentDebt()));
    }

    @Test
    @DisplayName("activeCard does not duplicate existing annual fee transaction")
    void activeCard_skipsExistingAnnualFeeTransaction() {
        CreditCard card = inactiveCardWithAnnualFee(new BigDecimal("1200.00"));
        CreditCardResponseDto response = new CreditCardResponseDto();

        when(cardRepository.findById(10)).thenReturn(Optional.of(card));
        when(cardTxnRepository.existsByCard_CardIdAndTxnType(10, TxnType.ANNUAL_FEE)).thenReturn(true);
        when(cardRepository.save(card)).thenReturn(card);
        when(mapper.toDto(card)).thenReturn(response);

        service.activeCard(10);

        verify(cardTxnRepository, never()).save(any(CardTransaction.class));
        assertEquals(0, BigDecimal.ZERO.compareTo(card.getCurrentDebt()));
    }

    private CreditCard inactiveCardWithAnnualFee(BigDecimal annualFee) {
        CardType cardType = new CardType();
        cardType.setAnnualFee(annualFee);

        CreditCard card = new CreditCard();
        card.setCardId(10);
        card.setCardType(cardType);
        card.setStatus(CardStatus.INACTIVE);
        card.setCurrentDebt(BigDecimal.ZERO);
        return card;
    }
}
