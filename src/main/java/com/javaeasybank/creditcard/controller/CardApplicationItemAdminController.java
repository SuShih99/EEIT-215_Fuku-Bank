package com.javaeasybank.creditcard.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.service.CardAppItemService;
import com.javaeasybank.creditcard.service.CardReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/card-application-items")
@RequiredArgsConstructor
public class CardApplicationItemAdminController {

    private final CardAppItemService cardAppItemService;
    private final CardReviewService cardReviewService;

    // 查全部明細（DTO）
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<List<CardApplicationItemResponseDto>>> getByApplicationId(
            @PathVariable Integer applicationId) {

        List<CardApplicationItemResponseDto> result = cardAppItemService.findByApplicationId(applicationId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 核准
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CardApplicationItemResponseDto>> approve(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        cardReviewService.approveItem(id)));
    }

    // 拒絕
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CardApplicationItemResponseDto>> reject(
            @PathVariable Integer id,
            @RequestParam String remark) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        cardReviewService.rejectItem(id, remark)));
    }
}
