package com.javaeasybank.creditcard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;

public interface CardAppItemRepository extends JpaRepository<CardApplicationItem, Integer> {

    List<CardApplicationItem> findByApplicationApplicationId(Integer applicationId);

    boolean existsByApplication_Customer_CustomerIdAndCardType_CardTypeIdAndApplication_Status(String customerId, Integer cardTypeId,
            CardApplicationStatus status);

}
