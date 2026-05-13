package com.javaeasybank.creditcard.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.entity.CardBill;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CardBillMapper {


    @Mapping(target = "customerName", source = "card.customer.name")
    @Mapping(target = "creditCardAccountNumber", source = "card.creditCardAccountNumber")
    CardBillResponseDto toDto(CardBill bill);

    List<CardBillResponseDto> toDtoList(List<CardBill> bills);

    

}