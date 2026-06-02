package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.dto.MerchantResponseDto;
import com.javaeasybank.creditcard.service.CardTxnService;
import com.javaeasybank.creditcard.service.MerchantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/card-txns")
public class CardTxnController {

    private final CardTxnService cardTxnService;
    private final SecurityUtil securityUtil;
    private final MerchantService merchantService;

    // 查詢自己交易列表
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardTxnResponseDto>>> getAllTransactions(
            Pageable pageable,
            @RequestHeader("Authorization") String authHeader) {

        String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
        Page<CardTxnResponseDto> page = cardTxnService.findByCustomerId(customerId, pageable);

        PageResponse<CardTxnResponseDto> response = PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> getTransaction(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTxnService.findById(id)));
    }

    // 新增交易
    @PostMapping
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> create(@RequestBody CardTxnRequestDto dto,
            @RequestHeader("Authorization") String authHeader) {
        dto.setCustomerId(securityUtil.getCustomerIdFromHeader(authHeader));
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", cardTxnService.create(dto)));
    }

    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<List<MerchantResponseDto>>> getAllMerchants() {
        return ResponseEntity.ok(ApiResponse.success(merchantService.getAllMerchants()));
    }
}
