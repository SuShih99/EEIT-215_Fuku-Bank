package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.entity.CardTransaction;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardTxnMapper {

    @Mapping(source = "merchant.merchantName", target = "merchantName")
    @Mapping(target = "cardNumber", expression = "java(maskCard(txn.getCard().getCardNumber()))")
    CardTxnResponseDto toDto(CardTransaction txn);

    List<CardTxnResponseDto> toDtoList(List<CardTransaction> list);

    
    default String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4)
            + " **** **** "
            + cardNumber.substring(cardNumber.length() - 4);
    }
}
