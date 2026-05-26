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
import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.enums.TxnType;
import com.javaeasybank.creditcard.mapper.CreditCardMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardTxnRepository;
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
    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("100000");

    private final CreditCardRepository cardRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardAppItemRepository itemRepository;
    private final CreditCardMapper mapper;
    private final AccountIntegrationService accountIntegrationService;
    private final AccountRepository accountRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CardTxnRepository cardTxnRepository;

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
        CardAccount cardAccount = resolveCardAccount(customer);

        CreditCard entity = mapper.toEntity(dto);
        entity.setCustomer(customer);
        entity.setCardType(
                cardTypeRepository.findById(dto.getCardTypeId())
                        .orElseThrow(() -> new BusinessException("CardType not found")));
        entity.setApplicationItem(resolveApplicationItem(dto.getApplicationItemId()));
        entity.setCurrentDebt(BigDecimal.ZERO);
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
        CustomerProfile customer = item.getApplication().getCustomer();
        CardAccount cardAccount = resolveCardAccount(customer, item.getApprovedLimit());

        CreditCard card = new CreditCard();
        card.setCustomer(customer);
        card.setCardType(item.getCardType());
        card.setApplicationItem(item);
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
        createAnnualFeeTransaction(card);
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

    private CardAccount resolveCardAccount(CustomerProfile customer) {
        return resolveCardAccount(customer, null);
    }

    private CardAccount resolveCardAccount(CustomerProfile customer, BigDecimal approvedLimit) {
        CardAccount cardAccount = cardAccountRepository.findByCustomer_CustomerId(customer.getCustomerId())
                .map(account -> updateExistingCardAccount(account, approvedLimit))
                .orElseGet(() -> createCardAccount(customer, approvedLimit));

        if (cardAccount.getAccountNumber() == null || cardAccount.getAccountNumber().isBlank()) {
            cardAccount.setAccountNumber(resolveCreditCardAccountNumber(customer.getCustomerId()));
            return cardAccountRepository.save(cardAccount);
        }

        return cardAccount;
    }

    private CardAccount createCardAccount(CustomerProfile customer, BigDecimal approvedLimit) {
        CardAccount account = new CardAccount();
        account.setCustomer(customer);
        account.setAccountNumber(resolveCreditCardAccountNumber(customer.getCustomerId()));
        account.setCreditLimit(resolveCreditLimit(approvedLimit));
        account.setStatementDay(DEFAULT_STATEMENT_DAY);
        account.setDueDays(DEFAULT_DUE_DAYS);
        return cardAccountRepository.save(account);
    }

    private CardAccount updateExistingCardAccount(CardAccount account, BigDecimal approvedLimit) {
        if (approvedLimit != null || account.getCreditLimit() == null) {
            account.setCreditLimit(resolveCreditLimit(approvedLimit));
        }
        if (account.getStatementDay() == null) {
            account.setStatementDay(DEFAULT_STATEMENT_DAY);
        }
        if (account.getDueDays() == null) {
            account.setDueDays(DEFAULT_DUE_DAYS);
        }
        return cardAccountRepository.save(account);
    }

    private BigDecimal resolveCreditLimit(BigDecimal approvedLimit) {
        return approvedLimit == null ? DEFAULT_CREDIT_LIMIT : approvedLimit;
    }

    private void createAnnualFeeTransaction(CreditCard card) {
        if (card.getCardId() == null
                || card.getCardType() == null
                || card.getCardType().getAnnualFee() == null
                || card.getCardType().getAnnualFee().compareTo(BigDecimal.ZERO) <= 0
                || cardTxnRepository.existsByCard_CardIdAndTxnType(card.getCardId(), TxnType.ANNUAL_FEE)) {
            return;
        }

        BigDecimal annualFee = card.getCardType().getAnnualFee();

        CardTransaction txn = new CardTransaction();
        txn.setCard(card);
        txn.setTxnAmount(annualFee);
        txn.setTxnType(TxnType.ANNUAL_FEE);
        txn.setTxnDate(LocalDateTime.now());
        txn.setDescription("信用卡年費");
        txn.setCashbackRate(BigDecimal.ZERO);
        txn.setCashbackAmount(BigDecimal.ZERO);

        card.setCurrentDebt(zeroIfNull(card.getCurrentDebt()).add(annualFee));
        cardTxnRepository.save(txn);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
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
    
}
