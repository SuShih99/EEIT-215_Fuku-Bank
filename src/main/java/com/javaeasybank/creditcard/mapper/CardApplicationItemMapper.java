package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CardApplicationItemRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationItem;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardApplicationItemMapper {

    // 新增用
    @Mapping(source = "cardTypeId", target = "cardType.cardTypeId")
    @Mapping(source = "applicationId", target = "application.applicationId")
    CardApplicationItem toEntity(CardApplicationItemRequestDto dto);

    // 查詢用
    @Mapping(source = "cardType.cardTypeName", target = "cardTypeName")
    CardApplicationItemResponseDto toDto(CardApplicationItem entity);

    // 更新用
    @Mapping(source = "cardTypeId", target = "cardType.cardTypeId")
    @Mapping(source = "applicationId", target = "application.applicationId")
    void updateEntityFromDto(CardApplicationItemRequestDto dto, @MappingTarget CardApplicationItem entity);

    List<CardApplicationItemResponseDto> toDtoList(List<CardApplicationItem> list);
}
