package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.service.BillService;
import com.javaeasybank.creditcard.service.CardTxnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/card-bills")
@RequiredArgsConstructor
public class CardBillController {

        private final BillService cardBillService;
        private final SecurityUtil securityUtil;
        private final CardTxnService cardTxnService;

        // 查全部帳單
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<CardBillResponseDto>>> getMyBills(Pageable pageable,
                        @RequestHeader("Authorization") String authHeader) {

                String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

                Page<CardBillResponseDto> page = cardBillService.getBillsByCustomerId(customerId, pageable);
                return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                                page.getContent(),
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements())));
        }

        // 查未出帳帳單
        @GetMapping("/unbilled")
        public ResponseEntity<ApiResponse<PageResponse<CardTxnResponseDto>>> getUnbilledBills(Pageable pageable,
                        @RequestHeader("Authorization") String authHeader) {
                String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

                Page<CardTxnResponseDto> page = cardTxnService.getUnbilledBillsByCustomerId(customerId, pageable);
                return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                                page.getContent(),
                                page.getNumber(),
                                page.getSize(),
                                page.getTotalElements())));
        }

        // 查某一期帳單、某張卡片的已出帳交易
        @GetMapping("/{billId}/transactions")
        public ResponseEntity<ApiResponse<List<CardTxnResponseDto>>> getBilledTransactions(
                        @PathVariable Integer billId,
                        @RequestParam Integer cardId,
                        @RequestHeader("Authorization") String authHeader) {

                String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

                List<CardTxnResponseDto> txns = cardTxnService.getBilledTransactions(customerId, billId, cardId);

                return ResponseEntity.ok(ApiResponse.success(txns));
        }

}
