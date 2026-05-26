package com.javaeasybank.creditcard.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentResponseDto;
import com.javaeasybank.creditcard.service.CardAppService;
import com.javaeasybank.creditcard.service.CardApplicationDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/card-application-documents")
public class CardApplicationDocumentAdminController {

    private final CardApplicationDocumentService documentService;
    private final CardAppService cardApplicationService;


    // 後台查某申請單附件
    @GetMapping("/{applicationId}/documents")
    public ApiResponse<List<CardApplicationDocumentResponseDto>> findByApplicationId(
            @PathVariable Integer applicationId) {

        return ApiResponse.success(documentService.findByApplicationId(applicationId));
    }

    // 後台刪除附件
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Integer documentId) {
        documentService.deleteDocument(documentId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{applicationId}/need-supplement")
public ApiResponse<Void> needSupplement(
        @PathVariable Integer applicationId,
        @RequestBody Map<String, String> request) {

    String remark = request.getOrDefault("remark", "");
    cardApplicationService.needSupplement(applicationId, remark);
    return ApiResponse.success(null);
}
}
