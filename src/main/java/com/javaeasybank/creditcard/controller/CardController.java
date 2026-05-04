package com.javaeasybank.creditcard.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.service.CreditCardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

	private final CreditCardService creditCardService;
	// 卡片列表
	//http://localhost:8080/card/my-cards
	@GetMapping
    public ResponseEntity<ApiResponse<List<CreditCardResponseDto>>> getMyCards() {
        return ResponseEntity.ok(ApiResponse.success(creditCardService.findAll()));
    }
	
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CreditCardResponseDto>> getCard(@PathVariable Integer id) {
	    return ResponseEntity.ok(ApiResponse.success(creditCardService.findById(id)));
	}
	
//	@GetMapping("/customer/{customerId}")
//	public ResponseEntity<List<CreditCard>> getByCustomer(@PathVariable Integer customerId) {
//	    return ResponseEntity.ok(creditCardService.findByCustomerId(customerId));
//	}
	
	
	
	
	
	
	
}
