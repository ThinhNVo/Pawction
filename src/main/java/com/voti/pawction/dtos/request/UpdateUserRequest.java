package com.voti.pawction.dtos.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String email;
}
