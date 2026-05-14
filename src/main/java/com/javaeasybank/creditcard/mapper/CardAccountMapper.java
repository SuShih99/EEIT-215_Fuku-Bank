package com.javaeasybank.creditcard.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardAccountResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class, uses = {CardSummaryMapper.class})
public interface CardAccountMapper {

    @Mapping(
        target = "availableCredit",
        expression = "java(calculateAvailableCredit(cardAccount))"
    )
    @Mapping(
        target = "customerId",
        source = "customer.customerId"
    )
    @Mapping(
        target = "customerName",
        source = "customer.name"
    )
    CardAccountResponseDto toDto(CardAccount cardAccount);

    default BigDecimal calculateAvailableCredit(CardAccount cardAccount) {
        return cardAccount.getCreditLimit().subtract(cardAccount.getCurrentDebt());
        
    }
}
