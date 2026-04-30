package com.javaeasybank.creditcard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.mapper.CardApplicationItemMapper;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CardAppItemService {

	private final CardAppItemRepository cardAppItemRepository;
	private final CardApplicationItemMapper mapper;
	
	
	public void deleteById(Integer id) {
		cardAppItemRepository.deleteById(id);
	}
	public List<CardApplicationItemResponseDto> findByApplicationId(Integer applicationId) {

        List<CardApplicationItem> list =
                cardAppItemRepository.findByApplicationApplicationId(applicationId);

        return mapper.toDtoList(list);
    }


}
