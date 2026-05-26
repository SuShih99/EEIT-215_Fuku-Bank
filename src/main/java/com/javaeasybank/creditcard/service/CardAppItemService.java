package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.enums.CardApplicationItemResult;
import com.javaeasybank.creditcard.mapper.CardApplicationItemMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CardAppItemService {

	private final CardAppItemRepository cardAppItemRepository;
	private final CardApplicationItemMapper mapper;
	private final CardAccountRepository cardAccountRepository;
	
	
	public void deleteById(Integer id) {
		cardAppItemRepository.deleteById(id);
	}
	public List<CardApplicationItemResponseDto> findByApplicationId(Integer applicationId) {

        List<CardApplicationItem> list =
            cardAppItemRepository.findByApplicationApplicationId(applicationId);

    for (CardApplicationItem item : list) {

    if (item.getResult() == CardApplicationItemResult.PENDING) {

        BigDecimal latestLimit = cardAccountRepository
                .findByCustomer_CustomerId(
                        item.getApplication().getCustomer().getCustomerId())
                .map(CardAccount::getCreditLimit)
                .orElse(item.getApprovedLimit());

        // 直接改 entity
        item.setApprovedLimit(latestLimit);
    }
}

// 最後再 mapper
return mapper.toDtoList(list);

}
}
