package com.javaeasybank.creditcard.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.javaeasybank.creditcard.dto.CardApplicationDocumentRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationDocumentResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationDocument;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class)
public interface CardApplicationDocumentMapper {

    CardApplicationDocumentResponseDto toDto(CardApplicationDocument entity);

    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    CardApplicationDocument toEntity(CardApplicationDocumentRequestDto dto);
}
