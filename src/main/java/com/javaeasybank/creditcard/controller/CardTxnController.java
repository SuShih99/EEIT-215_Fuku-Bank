package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.service.CardTxnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/card-txns")
public class CardTxnController {

    private final CardTxnService cardTxnService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTxnResponseDto>>> getAllTransactions() {
        return ResponseEntity.ok(ApiResponse.success(cardTxnService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> getTransaction(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTxnService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> create(@RequestBody CardTxnRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", cardTxnService.create(dto)));
    }
}
