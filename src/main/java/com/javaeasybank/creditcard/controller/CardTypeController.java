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
@RequestMapping("/user/card-types")
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTypeResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findAll()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTypeResponseDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findById(id)));
    }
}
