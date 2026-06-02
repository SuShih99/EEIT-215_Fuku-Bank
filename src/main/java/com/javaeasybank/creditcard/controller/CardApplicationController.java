package com.javaeasybank.creditcard.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.service.CardAppService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/card-applications")
@RequiredArgsConstructor
public class CardApplicationController {

    private final CardAppService cardAppService;
    private final SecurityUtil securityUtil;

    // 新增申請
    @PostMapping
    public ResponseEntity<ApiResponse<CardApplicationResponseDto>> apply(@RequestBody CardApplicationRequestDto request,
            @RequestHeader("Authorization") String authHeader) {
        request.setCustomerId(securityUtil.getCustomerIdFromHeader(authHeader));
        CardApplicationResponseDto created = cardAppService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Card application created", created));
    }

    // 查全部
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CardApplicationResponseDto>>> getMyApplications(Pageable pageable,
            @RequestHeader("Authorization") String authHeader) {
        String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.success(cardAppService.findMyApplications(customerId, pageable)));
    }

}
