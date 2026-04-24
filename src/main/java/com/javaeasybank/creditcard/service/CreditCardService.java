package com.javaeasybank.creditcard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.repository.CreditCardRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreditCardService {

	private final CreditCardRepository cardRepository;
	
	public List<CreditCard>findAll() {
		return cardRepository.findAll();
	}
	public void save(CreditCard card) {
		cardRepository.save(card);
	}
	public CreditCard findById(Integer id) {
		return cardRepository.findById(id).
orElseThrow(()->new BusinessException("Can not found the creditcard ID:"+id));
	}
	public void deleteById(Integer id) {
		cardRepository.deleteById(id);
	}
//	public List<CreditCard> findByCustomerId(Integer customerId) {
//		return creditCardRepository.findByCustomer_CustomerId(customerId);
//	}
	
	
}
