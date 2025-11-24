package com.voti.pawction.mappers;

import com.voti.pawction.dtos.request.UserRequest.RegisterUserRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel="spring")
public interface UserMapper {
    @Mapping(source = "userId", target = "userId")
    UserDto toDto(User user);
    User toEntity(RegisterUserRequest request);
}
