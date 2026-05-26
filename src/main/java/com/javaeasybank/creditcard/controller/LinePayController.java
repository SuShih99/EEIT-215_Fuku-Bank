package com.javaeasybank.creditcard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.creditcard.dto.LinePayRequestDto;
import com.javaeasybank.creditcard.service.LinePayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/linepay")
public class LinePayController {

    private final LinePayService linePayService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<?>>request(@RequestBody LinePayRequestDto request){
        return ResponseEntity.ok(ApiResponse.success("success", linePayService.request(request)));
        
    }
    @GetMapping("/confirm")
    public ResponseEntity<ApiResponse<?>> confirm(@RequestParam String transactionId, @RequestParam String orderId){
        return ResponseEntity.ok(ApiResponse.success(linePayService.confirm(transactionId, orderId)));
    
    }
}
