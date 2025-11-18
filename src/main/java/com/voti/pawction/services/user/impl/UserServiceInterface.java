package com.voti.pawction.services.user.impl;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;

public interface UserServiceInterface {
    /**
     *Registers a new user after validating inputs.
     *
     * @param name User name
     * @param email User new email
     * @param password User new password
     * @return userDto
     * @exception IllegalArgumentException bad data format
     * @exception UserNotFoundException User id not found
     * @exception UserEmailExistsException User email exists
     * @exception WeakPasswordException User's password invalid
     */
    UserDto register(String name, String email, String password);

    /**
     *Validates password strength against local policy.
     *
     * @param email User existing email
     * @param password User existing password
     * @return userDto
     * @exception UserNotFoundException if user email is not found
     * @exception InvalidCredentialsException if user email is not correct
     */
    UserDto Login(String email, String password);

    /**
     * Changes a user's password.
     *
     * @param userId User Id
     * @param oldPassword User old password
     * @param newPassword User new password
     * @return UserDto
     * @exception UserNotFoundException if User id not found
     * @exception InvalidCredentialsException if old password is the same as new and failed validation
     */
    UserDto ChangePassword(Long userId, String oldPassword, String newPassword);




}
