package com.javaeasybank.creditcard.mapper;



import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardSummaryDto;
import com.javaeasybank.creditcard.entity.CreditCard;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class)
public interface CardSummaryMapper {

    @Mapping(
        target="cardTypeName",
        source="cardType.cardTypeName")
    CardSummaryDto toDto(CreditCard card);


}
