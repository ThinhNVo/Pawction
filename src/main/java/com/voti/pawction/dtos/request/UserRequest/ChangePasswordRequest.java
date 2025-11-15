package com.voti.pawction.dtos.request.UserRequest;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
