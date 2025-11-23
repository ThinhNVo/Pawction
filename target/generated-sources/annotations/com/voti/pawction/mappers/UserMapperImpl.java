package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-21T19:54:27-0500",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(User user) {
        if ( user == null ) {
            return null;
        }

        String name = null;
        String email = null;

        name = user.getName();
        email = user.getEmail();

        Long id = null;

        UserDto userDto = new UserDto( id, name, email );

        return userDto;
    }

    @Override
    public User toEntity(UserDto userDto) {
        if ( userDto == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.name( userDto.getName() );
        user.email( userDto.getEmail() );

        return user.build();
    }
}
