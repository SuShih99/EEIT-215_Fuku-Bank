package com.javaeasybank.creditcard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.creditcard.repository.CardBillRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

	private final CardBillRepository cardBillRepository;
	
	
}
