package com.javaeasybank.creditcard.dto;

import java.time.LocalDateTime;

import lombok.Data;
@Data
public class CardApplicationDocumentResponseDto {

    private Integer documentId;

    private String fileUrl;

    private String fileName;

    private LocalDateTime uploadedAt;
}
