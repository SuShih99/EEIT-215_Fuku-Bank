package com.javaeasybank.creditcard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.repository.CardAppRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class CardAppService {

	private final CardAppRepository cardAppRepository;
	
	public List<CardApplication> findAll() {
		return cardAppRepository.findAll();
	}
	public CardApplication findById(Integer id) {
	    return cardAppRepository.findById(id)
	        .orElseThrow(() -> new BusinessException("Credit card application not found."));
	}
	public void save(CardApplication cardApplication) {
		cardAppRepository.save(cardApplication);
	}
	public void deleteById(Integer id) {
		cardAppRepository.deleteById(id);
	}
	
}
