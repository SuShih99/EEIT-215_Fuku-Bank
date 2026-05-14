package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.javaeasybank.account.dto.request.CreditCardAccountCreateRequest;
import com.javaeasybank.account.dto.response.CreditCardAccountResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CreditCardRequestDto;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.mapper.CreditCardMapper;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardTypeRepository;
import com.javaeasybank.creditcard.repository.CreditCardRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository cardRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardAppItemRepository itemRepository;
    private final CreditCardMapper mapper;
    private final AccountIntegrationService accountIntegrationService;

    // 查全部（回 DTO）
    public Page<CreditCardResponseDto> findAll(Pageable pageable, String keyword, CardStatus status) {
        return cardRepository.search(pageable, keyword, status).map(mapper::toDto);
    }

    // 查單筆
    public CreditCardResponseDto findById(Integer id) {
        CreditCard entity = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Can not found the creditcard ID:" + id));

        return mapper.toDto(entity);
    }

    // 新增(後臺用)
    public CreditCardResponseDto create(CreditCardRequestDto dto) {

        CreditCard entity = mapper.toEntity(dto);

        // 關聯處理
        entity.setCardType(
                cardTypeRepository.findById(dto.getCardTypeId())
                        .orElseThrow(() -> new BusinessException("CardType not found")));

        entity.setApplicationItem(
                itemRepository.findById(dto.getApplicationItemId())
                        .orElseThrow(() -> new BusinessException("ApplicationItem not found")));

        return mapper.toDto(cardRepository.save(entity));
    }

    // 更新
    public CreditCardResponseDto update(Integer id, CreditCardRequestDto dto) {

        CreditCard entity = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CreditCard not found"));

        //只更新額度
        if (dto.getCreditLimit()!=null) {
            entity.setCreditLimit(dto.getCreditLimit());
        }

        return mapper.toDto(cardRepository.save(entity));
    }

    // 刪除
    public void deleteById(Integer id) {
        cardRepository.deleteById(id);
    }

    // 查詢客戶卡片列表
    public List<CreditCardResponseDto> findByCustomerId(String customerId) {
        return mapper.toDtoList(cardRepository.findByCustomerCustomerId(customerId));
    }

    // 由item產生卡片(創建卡片)
    public void createFromApplicationItem(CardApplicationItem item) {

        CreditCard card = new CreditCard();
        card.setCustomer(item.getApplication().getCustomer());
        card.setCardType(item.getCardType());
        card.setCreditLimit(item.getApprovedLimit());

        // 初始消費額度為0
        card.setCurrentDebt(BigDecimal.ZERO);

        //開卡時間
        card.setCreateDate(LocalDateTime.now());


        card.setCardNumber(generateCardNumber());
        card.setExpiryDate(LocalDate.now().plusYears(5));

        // 預設狀態為未開通
        card.setStatus(CardStatus.INACTIVE);

        // 生成信用卡帳號
        CreditCardAccountCreateRequest accountRequest = new CreditCardAccountCreateRequest();
        accountRequest.setCustomerId(card.getCustomer().getCustomerId());

        CreditCardAccountResponse accountResponse = accountIntegrationService.createCreditCardAccount(accountRequest);
        card.setCreditCardAccountNumber(accountResponse.getCreditCardAccountNumber());

        // 存檔

        cardRepository.save(card);

        // 避免重複發卡
        item.setCreateCardFlag(true);

    }

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
    // 開通卡片
    public CreditCardResponseDto activeCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("找不到卡片"));

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new BusinessException("卡片已開通");
        }

        card.setStatus(CardStatus.ACTIVE);
        return mapper.toDto(cardRepository.save(card));
    }
    //停用卡片
    public CreditCardResponseDto blockCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("找不到卡片"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("卡片已停用");
        }
        if (card.getStatus() == CardStatus.INACTIVE) {
            throw new BusinessException("卡片尚未開通");
        }

        card.setStatus(CardStatus.BLOCKED);
        return mapper.toDto(cardRepository.save(card));
    }
    // 解除停用卡片
    public CreditCardResponseDto unblockCard(Integer id) {
        CreditCard card = cardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("找不到卡片"));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new BusinessException("卡片未停用");
        }

        card.setStatus(CardStatus.ACTIVE);
        return mapper.toDto(cardRepository.save(card));
    }

}
