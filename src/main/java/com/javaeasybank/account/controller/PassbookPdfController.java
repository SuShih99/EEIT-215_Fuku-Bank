package com.javaeasybank.account.controller;

import com.javaeasybank.account.service.PassbookPdfService;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/customer/accounts")
@RequiredArgsConstructor
public class PassbookPdfController {

    private final PassbookPdfService passbookPdfService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{accountNumber}/passbook/pdf")
    public ResponseEntity<byte[]> downloadPassbookPdf(HttpServletRequest request,
                                                       @PathVariable String accountNumber) {
        String customerId = jwtUtil.resolveCustomerId(request);
        byte[] pdf = passbookPdfService.generateEncryptedPassbookPdf(customerId, accountNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("e-passbook-" + accountNumber + ".pdf", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

}
