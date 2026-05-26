package com.javaeasybank.creditcard.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.service.CreditCardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/cards")
@RequiredArgsConstructor
public class CardController {

	private final CreditCardService creditCardService;
	private final SecurityUtil securityUtil;

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CreditCardResponseDto>> getCard(@PathVariable Integer id) {
	    return ResponseEntity.ok(ApiResponse.success(creditCardService.findById(id)));
	}
	
	@GetMapping("/my-cards")
	public ResponseEntity<ApiResponse<List<CreditCardResponseDto>>> getByCustomer(@RequestHeader("Authorization") String authHeader) {
	    String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
		
		
		return ResponseEntity.ok(ApiResponse.success(creditCardService.findByCustomerId(customerId)));
	}
	//開卡API
	@PatchMapping("/{id}/activate")
	public ResponseEntity<ApiResponse<CreditCardResponseDto>> activeCard(@PathVariable Integer id) {
	    return ResponseEntity.ok(ApiResponse.success(creditCardService.activeCard(id)));
		
	}
	
	
	
	
	
	
	
}
