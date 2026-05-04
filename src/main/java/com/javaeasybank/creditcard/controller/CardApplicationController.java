package com.javaeasybank.creditcard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.service.CardAppService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/card-applications")
@RequiredArgsConstructor
public class CardApplicationController {

    private final CardAppService cardAppService;

    // 新增申請
    @PostMapping
    public ResponseEntity<ApiResponse<CardApplicationResponseDto>> apply(@RequestBody CardApplicationRequestDto request) {
        CardApplicationResponseDto created = cardAppService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Card application created",created));
    }
}
