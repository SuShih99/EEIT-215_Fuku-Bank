package com.javaeasybank.creditcard.mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;

@MapperConfig(
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CentralMapperConfig {

}
