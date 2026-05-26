package com.javaeasybank.creditcard.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.creditcard.dto.CardTxnRequestDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.enums.TxnType;
import com.javaeasybank.creditcard.service.CardTxnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/card-txns")
@RequiredArgsConstructor
public class CardTxnAdminController {

    private final CardTxnService cardTxnService;

    // 查詢全部交易
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardTxnResponseDto>>> getAllTransactions(
            Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TxnType txnType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Page<CardTxnResponseDto> page = cardTxnService.search(
                keyword,
                txnType,
                startDate,
                endDate,
                pageable);
        PageResponse<CardTxnResponseDto> response = PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 查詢單一交易
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> getTransaction(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTxnService.findById(id)));
    }

    // 新增交易
    @PostMapping
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> createTransaction(@RequestBody CardTxnRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", cardTxnService.create(dto)));
    }

    // 更新交易
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> updateTransaction(
            @PathVariable Integer id,
            @RequestBody CardTxnRequestDto dto) {
        return ResponseEntity
                .ok(ApiResponse.success("Transaction updated successfully", cardTxnService.update(id, dto)));
    }

    // 退款交易
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<CardTxnResponseDto>> refund(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refund success",
                        cardTxnService.refund(id)));
    }

    // 刪除交易(備註:參考用，不可刪除交易)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Integer id) {
        cardTxnService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }
}
