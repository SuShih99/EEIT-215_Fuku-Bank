package com.javaeasybank.creditcard.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.service.CardAppService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/card-applications")
@RequiredArgsConstructor
public class CardApplicationController {

    private final CardAppService cardAppService;
    private final JwtUtil jwtUtil;

    // 新增申請
    @PostMapping
    public ResponseEntity<ApiResponse<CardApplicationResponseDto>> apply(@RequestBody CardApplicationRequestDto request,
                                                                        HttpServletRequest httpRequest) {
        request.setCustomerId(extractCustomerId(httpRequest));
        CardApplicationResponseDto created = cardAppService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Card application created",created));
    }
    //查全部
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CardApplicationResponseDto>>> getMyApplications(Pageable pageable,
                                                                                          HttpServletRequest request) {
        String customerId = extractCustomerId(request);

        return ResponseEntity.ok(ApiResponse.success(cardAppService.findMyApplications(customerId,pageable)));
    }

    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("Unable to get customerId from login user");
    }
}
