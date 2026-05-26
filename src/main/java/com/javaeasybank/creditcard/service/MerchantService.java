package com.javaeasybank.creditcard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.javaeasybank.creditcard.dto.MerchantResponseDto;
import com.javaeasybank.creditcard.entity.Merchant;
import com.javaeasybank.creditcard.repository.MerchantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public List<MerchantResponseDto> getAllMerchants() {
        
        List<Merchant> merchants = merchantRepository.findAll();

    return merchants.stream().map(merchant -> {

        MerchantResponseDto dto = new MerchantResponseDto();

        dto.setMerchantId(merchant.getMerchantId());
        dto.setMerchantName(merchant.getMerchantName());
        dto.setMerchantCategory(merchant.getMerchantCategory());

        return dto;

    }).toList();
    }
    
    
}
