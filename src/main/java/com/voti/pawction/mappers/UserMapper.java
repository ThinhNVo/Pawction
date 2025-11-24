package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel="spring")
public interface UserMapper {

    @Mapping(source = "userId", target = "id")
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
