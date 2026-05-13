package com.javaeasybank.creditcard.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.creditcard.dto.CreditCardRequestDto;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.creditcard.service.CreditCardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class CardAdminController {

    private final CreditCardService cardService;

    // 查詢卡列表
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CreditCardResponseDto>>> getAllCards(
        Pageable pageable,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) CardStatus status
    ) {
        Page<CreditCardResponseDto> Page = cardService.findAll(pageable, keyword, status);
        PageResponse<CreditCardResponseDto> response = PageResponse.of(
            Page.getContent(),
            Page.getNumber(),
            Page.getSize(),
            Page.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 查詢單一卡
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditCardResponseDto>> getCardById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardService.findById(id)));
    }

    // 新增卡
    @PostMapping
    public ResponseEntity<ApiResponse<CreditCardResponseDto>> createCard(@RequestBody CreditCardRequestDto dto) {
        CreditCardResponseDto created = cardService.create(dto);
        return ResponseEntity.ok(ApiResponse.success("Credit card created successfully", created));
    }

    // 更新卡
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditCardResponseDto>> updateCard(
            @PathVariable Integer id,
            @RequestBody CreditCardRequestDto dto
    ) {
        CreditCardResponseDto updated = cardService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Credit card updated successfully", updated));
    }

    // 刪除卡
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Integer id) {
        cardService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Credit card deleted successfully", null));
    }
    //停用卡片
    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<CreditCardResponseDto>> blockCard(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardService.blockCard(id)));
    }
    //啟用卡片
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<CreditCardResponseDto>> unblockCard(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardService.unblockCard(id)));
    }

}
