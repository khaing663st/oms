package com.kstr.oms.mapper;

import com.kstr.oms.domain.Order;
import com.kstr.oms.dto.OrderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    OrderDTO toDTO(Order order);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "sk", ignore = true)
    @Mapping(target = "entityType", ignore = true)
    @Mapping(target = "gsi1pk", ignore = true)
    @Mapping(target = "gsi1sk", ignore = true)
    @Mapping(target = "gsi2pk", ignore = true)
    @Mapping(target = "gsi2sk", ignore = true)
    Order toEntity(OrderDTO orderDTO);
}

