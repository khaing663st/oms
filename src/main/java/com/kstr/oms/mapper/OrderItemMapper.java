package com.kstr.oms.mapper;

import com.kstr.oms.domain.OrderItem;
import com.kstr.oms.dto.OrderItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemDTO toDTO(OrderItem orderItem);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "sk", ignore = true)
    @Mapping(target = "entityType", ignore = true)
    @Mapping(target = "gsi3pk", ignore = true)
    @Mapping(target = "gsi3sk", ignore = true)
    OrderItem toEntity(OrderItemDTO orderItemDTO);
}
