package com.voti.pawction.dtos.request.UserRequest;

import lombok.Data;

@Data
public class RegisterUserRequest {
    private String name;
    private String email;
    private String password;
}
