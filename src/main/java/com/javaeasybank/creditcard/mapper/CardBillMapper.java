package com.javaeasybank.creditcard.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.entity.CreditCard;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CardBillMapper {


    @Mapping(target = "customerName", source = "cardAccount.customer.name")
    @Mapping(target = "accountNumber", source = "cardAccount.accountNumber")
    @Mapping(target = "creditCardAccountNumber", source = "cardAccount.accountNumber")
    @Mapping(target = "creditLimit", source = "cardAccount.creditLimit")
    @Mapping(target = "availableCredit", expression = "java(calculateAvailableCredit(bill.getCardAccount()))")
    CardBillResponseDto toDto(CardBill bill);

    List<CardBillResponseDto> toDtoList(List<CardBill> bills);

    default BigDecimal calculateAvailableCredit(CardAccount cardAccount) {
        if (cardAccount == null || cardAccount.getCreditLimit() == null) {
            return BigDecimal.ZERO;
        }
        return cardAccount.getCreditLimit().subtract(calculateCurrentDebt(cardAccount));
    }

    default BigDecimal calculateCurrentDebt(CardAccount cardAccount) {
        if (cardAccount == null || cardAccount.getCards() == null) {
            return BigDecimal.ZERO;
        }

        return cardAccount.getCards().stream()
                .map(CreditCard::getCurrentDebt)
                .map(amount -> amount == null ? BigDecimal.ZERO : amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
