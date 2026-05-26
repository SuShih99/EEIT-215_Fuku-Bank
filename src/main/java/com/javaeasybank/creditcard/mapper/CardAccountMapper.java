package com.javaeasybank.creditcard.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardAccountResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CreditCard;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class, uses = {CardSummaryMapper.class})
public interface CardAccountMapper {

    @Mapping(
        target = "availableCredit",
        expression = "java(calculateAvailableCredit(cardAccount))"
    )
    @Mapping(
        target = "currentDebt",
        expression = "java(calculateCurrentDebt(cardAccount))"
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
        if (cardAccount == null || cardAccount.getCreditLimit() == null) {
            return BigDecimal.ZERO;
        }
        return cardAccount.getCreditLimit().subtract(calculateCurrentDebt(cardAccount));
        
    }

    default BigDecimal calculateCurrentDebt(CardAccount cardAccount) {
        if (cardAccount == null) {
            return BigDecimal.ZERO;
        }

        List<CreditCard> cards = cardAccount.getCards();
        if (cards == null || cards.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cards.stream()
                .map(CreditCard::getCurrentDebt)
                .map(amount -> amount == null ? BigDecimal.ZERO : amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
