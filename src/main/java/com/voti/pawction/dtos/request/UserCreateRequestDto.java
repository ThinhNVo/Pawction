package com.voti.pawction.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class UserCreateRequestDto {
    private String name;
    private String email;
    private String password;
}
