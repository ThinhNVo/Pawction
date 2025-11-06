package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
