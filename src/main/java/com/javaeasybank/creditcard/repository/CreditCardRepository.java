package com.javaeasybank.creditcard.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.javaeasybank.creditcard.entity.CreditCard;
import com.javaeasybank.creditcard.enums.CardStatus;

public interface CreditCardRepository extends JpaRepository<CreditCard, Integer> {

	boolean existsByCardType_CardTypeId(Integer cardTypeId);

    boolean existsByCardNumber(String cardNumber);

    List<CreditCard> findByCustomerCustomerId(String customerId);


    @Query("""
    SELECT c
    FROM CreditCard c
    WHERE
        (:keyword IS NULL
         OR c.customer.name LIKE %:keyword%
         OR c.cardType.cardTypeName LIKE %:keyword%)
    AND
        (:status IS NULL
         OR c.status = :status)
""")
    Page<CreditCard> search(Pageable pageable,@Param("keyword") String keyword,
        @Param("status") CardStatus status
        );
}
