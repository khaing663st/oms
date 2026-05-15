package com.kstr.oms.mapper;

import com.kstr.oms.domain.User;
import com.kstr.oms.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "sk", ignore = true)
    User toEntity(UserDTO userDTO);

}

