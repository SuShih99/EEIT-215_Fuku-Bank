package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentResponseDto;
import com.javaeasybank.creditcard.service.CardApplicationDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/card-application-documents")
public class CardApplicationDocumentController {

    private final CardApplicationDocumentService documentService;
    private final SecurityUtil securityUtil;



    // 新增附件
    @PostMapping("/{applicationId}/documents")
public ApiResponse<CardApplicationDocumentResponseDto> addDocument(
        @PathVariable Integer applicationId,
        @RequestBody CardApplicationDocumentRequestDto dto,@RequestHeader("Authorization") String authHeader) {

    String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

    return ApiResponse.success(documentService.addDocument(customerId, applicationId, dto));
}

    // 查詢某申請單附件
    @GetMapping("/{applicationId}/documents")
public ApiResponse<List<CardApplicationDocumentResponseDto>> findByApplicationId(
        @PathVariable Integer applicationId,@RequestHeader("Authorization") String authHeader) {

    String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

    return ApiResponse.success(documentService.findByApplicationId(customerId, applicationId));
}

    // 刪除附件
    @DeleteMapping("/documents/{documentId}")
public ApiResponse<Void> deleteDocument(@PathVariable Integer documentId,@RequestHeader("Authorization") String authHeader) {

    String customerId = securityUtil.getCustomerIdFromHeader(authHeader);

    documentService.deleteDocument(customerId, documentId);
    return ApiResponse.success(null);
}
}
