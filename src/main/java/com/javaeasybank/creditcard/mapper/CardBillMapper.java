package com.javaeasybank.creditcard.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardBillResponseDto;
import com.javaeasybank.creditcard.dto.CardSummaryDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.entity.CreditCard;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CardBillMapper {


    @Mapping(target = "customerName", expression = "java(resolveCustomerName(bill))")
    @Mapping(target = "accountNumber", expression = "java(resolveAccountNumber(bill))")
    @Mapping(target = "creditCardAccountNumber", expression = "java(resolveAccountNumber(bill))")
    @Mapping(target = "creditLimit", expression = "java(resolveCreditLimit(bill))")
    @Mapping(target = "availableCredit", expression = "java(calculateAvailableCredit(resolveCardAccount(bill)))")
    @Mapping(target = "cards", expression = "java(resolveCards(bill))")
    CardBillResponseDto toDto(CardBill bill);

    List<CardBillResponseDto> toDtoList(List<CardBill> bills);

    default CardAccount resolveCardAccount(CardBill bill) {
        if (bill == null) {
            return null;
        }

        if (bill.getCardAccount() != null) {
            return bill.getCardAccount();
        }

        return bill.getCard() == null ? null : bill.getCard().getCardAccount();
    }

    default String resolveCustomerName(CardBill bill) {
        CardAccount cardAccount = resolveCardAccount(bill);
        if (cardAccount != null && cardAccount.getCustomer() != null) {
            return cardAccount.getCustomer().getName();
        }

        if (bill != null && bill.getCard() != null && bill.getCard().getCustomer() != null) {
            return bill.getCard().getCustomer().getName();
        }

        return null;
    }

    default String resolveAccountNumber(CardBill bill) {
        CardAccount cardAccount = resolveCardAccount(bill);
        return cardAccount == null ? null : cardAccount.getAccountNumber();
    }

    default BigDecimal resolveCreditLimit(CardBill bill) {
        CardAccount cardAccount = resolveCardAccount(bill);
        return cardAccount == null ? null : cardAccount.getCreditLimit();
    }

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
    default List<CardSummaryDto> resolveCards(CardBill bill) {
    CardAccount cardAccount = resolveCardAccount(bill);

    if (cardAccount == null || cardAccount.getCards() == null) {
        return List.of();
    }

    return cardAccount.getCards()
            .stream()
            .map(card -> new CardSummaryDto(
                    card.getCardId(),
                    card.getCardNumber(),
                    card.getCardType() == null ? null : card.getCardType().getCardTypeName()
            ))
            .toList();
}

}
