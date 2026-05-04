package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardTypeResponseDto;
import com.javaeasybank.creditcard.service.CardTypeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/card-types")
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    // 🔹 1. 查全部卡別
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTypeResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findAll()));
    }

    // 🔹 2. 查單一卡別
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTypeResponseDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findById(id)));
    }
}
