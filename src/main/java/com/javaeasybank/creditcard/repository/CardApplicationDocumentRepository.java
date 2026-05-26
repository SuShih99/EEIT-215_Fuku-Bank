package com.javaeasybank.creditcard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardApplicationDocument;

public interface CardApplicationDocumentRepository extends JpaRepository<CardApplicationDocument, Integer>{

    List<CardApplicationDocument> findByApplicationApplicationId(Integer applicationId);

    
}
