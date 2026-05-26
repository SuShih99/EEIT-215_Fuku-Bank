package com.javaeasybank.creditcard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentResponseDto;
import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.entity.CardApplicationDocument;
import com.javaeasybank.creditcard.mapper.CardApplicationDocumentMapper;
import com.javaeasybank.creditcard.repository.CardAppRepository;
import com.javaeasybank.creditcard.repository.CardApplicationDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CardApplicationDocumentService {

    private final CardAppRepository cardApplicationRepository;
    private final CardApplicationDocumentRepository documentRepository;
    private final CardApplicationDocumentMapper mapper;

    public CardApplicationDocumentResponseDto addDocument(
            String customerId,
            Integer applicationId,
            CardApplicationDocumentRequestDto dto) {

        CardApplication application = cardApplicationRepository
                .findByApplicationIdAndCustomerCustomerId(applicationId, customerId)
                .orElseThrow(() -> new BusinessException("找不到信用卡申請單"));

        CardApplicationDocument document = mapper.toEntity(dto);
        document.setApplication(application);

        CardApplicationDocument saved = documentRepository.save(document);

        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CardApplicationDocumentResponseDto> findByApplicationId(
            String customerId,
            Integer applicationId) {

        cardApplicationRepository
                .findByApplicationIdAndCustomerCustomerId(applicationId, customerId)
                .orElseThrow(() -> new BusinessException("找不到信用卡申請單"));

        return documentRepository.findByApplicationApplicationId(applicationId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public void deleteDocument(String customerId, Integer documentId) {

        CardApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("找不到附件"));

        if (!document.getApplication().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException("無權限刪除此附件");
        }

        documentRepository.delete(document);
    }

    //後台
    public void deleteDocument(Integer documentId) {
        CardApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found"));

        documentRepository.delete(document);
    }
    //後台
    public List<CardApplicationDocumentResponseDto> findByApplicationId(Integer applicationId) {
        cardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Card application not found"));

        return documentRepository.findByApplicationApplicationId(applicationId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}
