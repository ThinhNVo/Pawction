package com.voti.pawction.mappers;

import com.voti.pawction.dtos.request.UserRequest.RegisterUserRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-30T22:09:10-0500",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long userId = null;
        String name = null;
        String email = null;

        userId = user.getUserId();
        name = user.getName();
        email = user.getEmail();

        UserDto userDto = new UserDto( userId, name, email );

        return userDto;
    }

    @Override
    public User toEntity(RegisterUserRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.name( request.getName() );
        user.email( request.getEmail() );

        return user.build();
    }
}
