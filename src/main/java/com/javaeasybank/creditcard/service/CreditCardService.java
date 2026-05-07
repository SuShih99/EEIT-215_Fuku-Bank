package com.javaeasybank.creditcard.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CreditCardRequestDto;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.entity.CreditCard;
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

    // 查全部（回 DTO）
    public List<CreditCardResponseDto> findAll() {
        return mapper.toDtoList(cardRepository.findAll());
    }

    // 查單筆
    public CreditCardResponseDto findById(Integer id) {
        CreditCard entity = cardRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Can not found the creditcard ID:" + id));

        return mapper.toDto(entity);
    }

    // 新增
    public CreditCardResponseDto create(CreditCardRequestDto dto) {

        CreditCard entity = mapper.toEntity(dto);

        // 關聯處理
        entity.setCardType(
            cardTypeRepository.findById(dto.getCardTypeId())
                .orElseThrow(() -> new BusinessException("CardType not found"))
        );

        entity.setApplicationItem(
            itemRepository.findById(dto.getApplicationItemId())
                .orElseThrow(() -> new BusinessException("ApplicationItem not found"))
        );

        return mapper.toDto(cardRepository.save(entity));
    }

    // 更新
    public CreditCardResponseDto update(Integer id, CreditCardRequestDto dto) {

        CreditCard entity = cardRepository.findById(id)
            .orElseThrow(() -> new BusinessException("CreditCard not found"));

        // 基本欄位更新
        mapper.updateEntityFromDto(dto, entity);

        // 關聯更新
        if (dto.getCardTypeId() != null) {
            entity.setCardType(
                cardTypeRepository.findById(dto.getCardTypeId())
                    .orElseThrow(() -> new BusinessException("CardType not found"))
            );
        }

        if (dto.getApplicationItemId() != null) {
            entity.setApplicationItem(
                itemRepository.findById(dto.getApplicationItemId())
                    .orElseThrow(() -> new BusinessException("ApplicationItem not found"))
            );
        }

        return mapper.toDto(cardRepository.save(entity));
    }

    // 刪除
    public void deleteById(Integer id) {
        cardRepository.deleteById(id);
    }
	//查詢客戶卡片列表
	public List<CreditCardResponseDto> findByCustomerId(Integer customerId) {
		return mapper.toDtoList(cardRepository.findByCustomerCustomerId(customerId));
	}
	//由item產生卡片
    public void createFromApplicationItem(CardApplicationItem item){

        CreditCard card = new CreditCard();
        card.setCustomer(item.getApplication().getCustomer());
    card.setCardType(item.getCardType());
    card.setCreditLimit(item.getApprovedLimit());

    card.setCardNumber(generateCardNumber());
    card.setExpiryDate(LocalDate.now().plusYears(5));

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
	
}
