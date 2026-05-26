package com.javaeasybank.creditcard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CardAccountResponseDto;
import com.javaeasybank.creditcard.service.CardAccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/card-accounts")
@RequiredArgsConstructor
public class CardAccountController {

    private final CardAccountService cardAccountService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<CardAccountResponseDto>> getMyAccount(@RequestHeader("Authorization") String authHeader) {
        String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.success(cardAccountService.getCardAccountById(customerId)));
    }

}
