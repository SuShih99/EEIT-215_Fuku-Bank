package com.javaeasybank.creditcard.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.CardTypeRequestDto;
import com.javaeasybank.creditcard.dto.CardTypeResponseDto;
import com.javaeasybank.creditcard.service.CardTypeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/card-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CardTypeAdminController {

    private final CardTypeService cardTypeService;

    // 查詢所有卡別
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTypeResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findAll()));
    }

    // 查詢單一卡別
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardTypeResponseDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(cardTypeService.findById(id)));
    }

    // 新增卡別（含圖片）
    @PostMapping
    public ResponseEntity<ApiResponse<CardTypeResponseDto>> create(
    		@RequestBody CardTypeRequestDto req) throws IOException {

        CardTypeResponseDto cardTypeDto = cardTypeService.createCardType(req);

        return ResponseEntity.ok(ApiResponse.success("Card type created",cardTypeDto));
    }
    // 上傳圖片
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
    		@RequestParam("file") MultipartFile file) throws IOException {

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get("uploads/" + filename);

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        String url = "uploads/" + filename;

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    // 更新卡別（圖片可選）
    @PutMapping("/{id}")
public ResponseEntity<ApiResponse<CardTypeResponseDto>> update(
        @PathVariable Integer id,
        @RequestBody CardTypeRequestDto req) {

    CardTypeResponseDto card = cardTypeService.updateCardType(id, req);

    return ResponseEntity.ok(ApiResponse.success("Card type updated", card));
}

    // 刪除卡別
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletebyId(@PathVariable Integer id) {
        cardTypeService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Card type deleted", null));
    }
}
