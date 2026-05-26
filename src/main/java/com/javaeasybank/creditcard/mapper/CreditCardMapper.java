package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CreditCardRequestDto;
import com.javaeasybank.creditcard.dto.CreditCardResponseDto;
import com.javaeasybank.creditcard.entity.CreditCard;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreditCardMapper {

    // 新增用
    //不須Mapping，交給Service
    @Mapping(source = "cardTypeId", target = "cardType.cardTypeId")
    @Mapping(source = "applicationItemId", target = "applicationItem.itemId")
    CreditCard toEntity(CreditCardRequestDto dto);
    
    // 查詢用
    @Mapping(target = "cardNumber", qualifiedByName = "maskCard")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "cardAccount.creditLimit", target = "creditLimit")
    CreditCardResponseDto toDto(CreditCard entity);

    // 更新用
    //不須Mapping，交給Service
    @Mapping(source = "cardTypeId", target = "cardType.cardTypeId")
    @Mapping(source = "applicationItemId", target = "applicationItem.itemId")
    void updateEntityFromDto(CreditCardRequestDto dto, @MappingTarget CreditCard entity);

    List<CreditCardResponseDto> toDtoList(List<CreditCard> list);

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
