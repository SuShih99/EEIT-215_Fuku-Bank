package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.account.dto.request.CreditCardAccountCreateRequest;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CreditCardRequestDto;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.mapper.CreditCardMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardTypeRepository;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreditCardService {

    private static final int DEFAULT_STATEMENT_DAY = 5;
    private static final int DEFAULT_DUE_DAYS = 14;

    private final CreditCardRepository cardRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardAppItemRepository itemRepository;
    private final CreditCardMapper mapper;
    private final AccountIntegrationService accountIntegrationService;
    private final AccountRepository accountRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public Page<CreditCardResponseDto> findAll(Pageable pageable, String keyword, CardStatus status) {
        return cardRepository.search(pageable, keyword, status).map(mapper::toDto);
    }

    public CreditCardResponseDto findById(Integer id) {
        CreditCard entity = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credit card not found: " + id));

        return mapper.toDto(entity);
    }

    public CreditCardResponseDto create(CreditCardRequestDto dto) {
        CustomerProfile customer = customerProfileRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found: " + dto.getCustomerId()));
        BigDecimal creditLimit = zeroIfNull(dto.getCreditLimit());
        CardAccount cardAccount = resolveCardAccount(customer, creditLimit);

        CreditCard entity = mapper.toEntity(dto);
        entity.setCustomer(customer);
        entity.setCardType(
                cardTypeRepository.findById(dto.getCardTypeId())
                        .orElseThrow(() -> new BusinessException("CardType not found")));
        entity.setApplicationItem(resolveApplicationItem(dto.getApplicationItemId()));
        entity.setCreditLimit(creditLimit);
        entity.setCurrentDebt(zeroIfNull(dto.getCurrentDebt()));
        entity.setCreateDate(LocalDateTime.now());
        entity.setCardNumber(generateCardNumber());
        entity.setExpiryDate(LocalDate.now().plusYears(5));
        entity.setStatus(CardStatus.INACTIVE);
        entity.setCardAccount(cardAccount);
        entity.setCreditCardAccountNumber(cardAccount.getAccountNumber());

        return mapper.toDto(cardRepository.save(entity));
    }

    public CreditCardResponseDto update(Integer id, CreditCardRequestDto dto) {
        CreditCard entity = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CreditCard not found"));

        if (dto.getCreditLimit() != null) {
            adjustCardAccountLimit(entity, dto.getCreditLimit());
            entity.setCreditLimit(dto.getCreditLimit());
        }

        return mapper.toDto(cardRepository.save(entity));
    }

    public void deleteById(Integer id) {
        cardRepository.deleteById(id);
    }

    public List<CreditCardResponseDto> findByCustomerId(String customerId) {
        return mapper.toDtoList(cardRepository.findByCustomerCustomerId(customerId));
    }

    public void createFromApplicationItem(CardApplicationItem item) {
        if (Boolean.TRUE.equals(item.getCreateCardFlag())) {
            throw new BusinessException("Credit card has already been created for this application item");
        }
        if (item.getApprovedLimit() == null) {
            throw new BusinessException("Approved limit is required before creating a credit card");
        }

        CustomerProfile customer = item.getApplication().getCustomer();
        CardAccount cardAccount = resolveCardAccount(customer, item.getApprovedLimit());

        CreditCard card = new CreditCard();
        card.setCustomer(customer);
        card.setCardType(item.getCardType());
        card.setApplicationItem(item);
        card.setCreditLimit(item.getApprovedLimit());
        card.setCurrentDebt(BigDecimal.ZERO);
        card.setCreateDate(LocalDateTime.now());
        card.setCardNumber(generateCardNumber());
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setStatus(CardStatus.INACTIVE);
        card.setCardAccount(cardAccount);
        card.setCreditCardAccountNumber(cardAccount.getAccountNumber());

        cardRepository.save(card);
        item.setCreateCardFlag(true);
    }

    public CreditCardResponseDto activeCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credit card not found"));

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new BusinessException("Credit card is already active");
        }

        card.setStatus(CardStatus.ACTIVE);
        return mapper.toDto(cardRepository.save(card));
    }

    public CreditCardResponseDto blockCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credit card not found"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("Credit card is already blocked");
        }
        if (card.getStatus() == CardStatus.INACTIVE) {
            throw new BusinessException("Inactive credit card cannot be blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        return mapper.toDto(cardRepository.save(card));
    }

    public CreditCardResponseDto unblockCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Credit card not found"));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new BusinessException("Only blocked credit cards can be unblocked");
        }

        card.setStatus(CardStatus.ACTIVE);
        return mapper.toDto(cardRepository.save(card));
    }

    private CardApplicationItem resolveApplicationItem(Integer applicationItemId) {
        if (applicationItemId == null) {
            return null;
        }
        return itemRepository.findById(applicationItemId)
                .orElseThrow(() -> new BusinessException("ApplicationItem not found"));
    }

    private CardAccount resolveCardAccount(CustomerProfile customer, BigDecimal addedCreditLimit) {
        CardAccount cardAccount = cardAccountRepository.findByCustomer_CustomerId(customer.getCustomerId())
                .map(account -> updateExistingCardAccount(account, addedCreditLimit))
                .orElseGet(() -> createCardAccount(customer, addedCreditLimit));

        if (cardAccount.getAccountNumber() == null || cardAccount.getAccountNumber().isBlank()) {
            cardAccount.setAccountNumber(resolveCreditCardAccountNumber(customer.getCustomerId()));
            return cardAccountRepository.save(cardAccount);
        }

        return cardAccount;
    }

    private CardAccount createCardAccount(CustomerProfile customer, BigDecimal creditLimit) {
        CardAccount account = new CardAccount();
        account.setCustomer(customer);
        account.setAccountNumber(resolveCreditCardAccountNumber(customer.getCustomerId()));
        account.setCreditLimit(zeroIfNull(creditLimit));
        account.setStatementDay(DEFAULT_STATEMENT_DAY);
        account.setDueDays(DEFAULT_DUE_DAYS);
        return cardAccountRepository.save(account);
    }

    private CardAccount updateExistingCardAccount(CardAccount account, BigDecimal addedCreditLimit) {
        account.setCreditLimit(zeroIfNull(account.getCreditLimit()).add(zeroIfNull(addedCreditLimit)));
        if (account.getStatementDay() == null) {
            account.setStatementDay(DEFAULT_STATEMENT_DAY);
        }
        if (account.getDueDays() == null) {
            account.setDueDays(DEFAULT_DUE_DAYS);
        }
        return cardAccountRepository.save(account);
    }

    private void adjustCardAccountLimit(CreditCard card, BigDecimal newCreditLimit) {
        CardAccount cardAccount = card.getCardAccount();
        if (cardAccount == null) {
            return;
        }

        BigDecimal oldCreditLimit = zeroIfNull(card.getCreditLimit());
        BigDecimal limitDifference = newCreditLimit.subtract(oldCreditLimit);
        cardAccount.setCreditLimit(zeroIfNull(cardAccount.getCreditLimit()).add(limitDifference));
        cardAccountRepository.save(cardAccount);
    }
    
    private String resolveCreditCardAccountNumber(String customerId) {
        return accountRepository.findFirstByCustomerIdAndAccountType(customerId, AccountType.CREDIT_CARD)
                .map(account -> account.getAccountNumber())
                .orElseGet(() -> {
                    CreditCardAccountCreateRequest accountRequest = new CreditCardAccountCreateRequest();
                    accountRequest.setCustomerId(customerId);
                    return accountIntegrationService.createCreditCardAccount(accountRequest)
                            .getCreditCardAccountNumber();
                });
    }
    //產生卡號
    private String generateCardNumber() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 10; i++) {
            String cardNumber = "4" + random.nextLong(100_000_000_000_000L, 1_000_000_000_000_000L);
            if (!cardRepository.existsByCardNumber(cardNumber)) {
                return cardNumber;
            }
        }

        throw new BusinessException("Unable to generate unique card number");
    }
    //回傳0，如果為Null
    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
