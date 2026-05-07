package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CardTxnResponseDto;
import com.javaeasybank.creditcard.entity.CardTransaction;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardTxnMapper {

    @Mapping(source = "merchant.merchantName", target = "merchantName")
    @Mapping(source = "card.customer.name", target = "customerName")
    @Mapping(source = "card.cardNumber", target = "cardNumber", qualifiedByName = "maskCard")
    @Mapping(source = "refTxn.txnId", target = "refTxnId")
    CardTxnResponseDto toDto(CardTransaction txn);

    List<CardTxnResponseDto> toDtoList(List<CardTransaction> list);

    @Named("maskCard")
    default String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4)
            + " **** **** "
            + cardNumber.substring(cardNumber.length() - 4);
    }
}
