package com.javaeasybank.creditcard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardApplicationItem;

public interface CardAppItemRepository extends JpaRepository<CardApplicationItem, Integer> {

    List<CardApplicationItem> findByApplicationApplicationId(Integer applicationId);

}
