package com.javaeasybank.creditcard.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.enums.BillStatus;
import com.javaeasybank.creditcard.service.BillService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/card-bills")
@RequiredArgsConstructor
public class CardBillAdminController {

    private final BillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardBillResponseDto>>> getAllBills(
            @RequestParam(required = false) String customerName,
        @RequestParam(required = false) String billingMonth,
        @RequestParam(required = false) BillStatus billStatus,
        Pageable pageable) {
        Page<CardBillResponseDto> page = billService.getBills(customerName, billingMonth, billStatus, pageable);
        PageResponse<CardBillResponseDto> response = PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Integer>> generateBills() {
        Integer count = billService.generateBills();
        return ResponseEntity.ok(ApiResponse.success("成功產生 " + count + " 筆帳單", count));
    }

}
