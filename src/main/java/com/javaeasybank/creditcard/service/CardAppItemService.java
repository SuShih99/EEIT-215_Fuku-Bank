package com.javaeasybank.creditcard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CardAppItemService {

	private final CardAppItemRepository cardAppItemRepository;
	
	public void save(CardApplicationItem cardApplicationItem) {
		cardAppItemRepository.save(cardApplicationItem);
	}
	public void deleteById(Integer id) {
		cardAppItemRepository.deleteById(id);
	}
	public CardApplicationItem findById(Integer id) {
		return cardAppItemRepository.findById(id).orElseThrow(()->new BusinessException("Application item not found"));
	}

}
