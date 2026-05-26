package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CardTypeRequestDto;
import com.javaeasybank.creditcard.dto.CardTypeResponseDto;
import com.javaeasybank.creditcard.entity.CardType;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardTypeMapper {
    
    CardTypeResponseDto toDto(CardType entity);

    CardType toEntity(CardTypeRequestDto dto);

    // 更新用
    void updateEntityFromDto(CardTypeRequestDto dto, @MappingTarget CardType entity);

    List<CardTypeResponseDto> toDtoList(List<CardType> list);

}
