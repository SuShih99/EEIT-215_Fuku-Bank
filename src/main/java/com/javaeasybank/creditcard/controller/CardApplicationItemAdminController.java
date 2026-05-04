package com.javaeasybank.creditcard.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.service.CardAppItemService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/card-application-items")
@RequiredArgsConstructor
public class CardApplicationItemAdminController {

    private final CardAppItemService cardAppItemService;

    // 查全部明細（DTO）
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<List<CardApplicationItemResponseDto>>> getByApplicationId(
            @PathVariable Integer applicationId) {

        List<CardApplicationItemResponseDto> result =
                cardAppItemService.findByApplicationId(applicationId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
    

}
