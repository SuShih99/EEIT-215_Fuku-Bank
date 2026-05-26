package com.javaeasybank.risk.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.request.ReviewDecisionRequest;
import com.javaeasybank.risk.dto.response.ReviewTaskResponse;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.service.ReviewTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/risk/reviewtask")
public class ReviewTaskController {

    final ReviewTaskService reviewTaskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CISO', 'CFDM')")
    public ResponseEntity<ApiResponse<Page<ReviewTaskResponse>>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BusinessScene scene,
            @RequestParam(required = false) Integer priority,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewTaskService.findAll(status, scene, priority, pageable)));
    }

    @PutMapping("/{taskId}/decision")
    @PreAuthorize("hasAnyRole('CISO', 'CFDM')")
    public ResponseEntity<ApiResponse<Void>> makeDecision(
            @PathVariable Long taskId,
            @RequestBody @Valid ReviewDecisionRequest request) {

        reviewTaskService.makeDecision(taskId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }


    @PutMapping("/{taskId}/start")
    @PreAuthorize("hasAnyRole('CISO', 'CFDM')")
    public ResponseEntity<ApiResponse<Void>> startProcessing(
            @PathVariable Long taskId,
            Authentication authentication) {

        // 2. 取得管理員的標識（對應前端 Vue 的 currentUser.email 或 username）
        String currentAdminIdentifier = authentication.getName();

        // 3. 將管理員標識傳入 Service
        reviewTaskService.startProcessing(taskId, currentAdminIdentifier);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}