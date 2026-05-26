package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.FavoriteAccountRequest;
import com.javaeasybank.account.dto.request.FavoriteAccountUpdateRequest;
import com.javaeasybank.account.dto.response.FavoriteAccountResponse;
import com.javaeasybank.account.service.FavoriteAccountService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customer/favorite-accounts")
@RequiredArgsConstructor
public class FavoriteAccountController {

    private final FavoriteAccountService favoriteAccountService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteAccountResponse>>> list(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(favoriteAccountService.getByCustomerId(customerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteAccountResponse>> create(
            @Valid @RequestBody FavoriteAccountRequest body,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        FavoriteAccountResponse response = favoriteAccountService.create(customerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FavoriteAccountResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody FavoriteAccountUpdateRequest body,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        FavoriteAccountResponse response = favoriteAccountService.update(customerId, id, body);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        favoriteAccountService.delete(customerId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
