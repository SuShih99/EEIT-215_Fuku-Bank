package com.javaeasybank.creditcard.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.entity.CardApplication;

@Mapper(componentModel = "spring",config = CentralMapperConfig.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardApplicationMapper {

	// 查詢用
	@Mapping(source = "customerProfile.customerId", target = "customerId")
    @Mapping(source = "customerProfile.name", target = "customerName")
    CardApplicationResponseDto toDto(CardApplication app);
	// 更新用
	@Mapping(source = "customerId", target = "customerProfile.customerId")
    CardApplication toEntity(CardApplicationRequestDto requestDto);

	List<CardApplicationResponseDto> toDtoList(List<CardApplication> all);
}
