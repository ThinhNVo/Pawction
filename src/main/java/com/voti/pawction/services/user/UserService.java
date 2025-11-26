package com.voti.pawction.services.user;
import org.springframework.transaction.annotation.Transactional;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;
import com.voti.pawction.mappers.UserMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.services.user.impl.UserServiceInterface;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class UserService implements UserServiceInterface {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserMapper userMapper;

    /**
     *Registers a new user after validating inputs.
     *
     * @param name User name
     * @param email User new email
     * @param password User new password
     * @return userDto
     * @exception IllegalArgumentException bad data format
     * @exception UserEmailExistsException User email exists
     * @exception WeakPasswordException User's password invalid
     */
    @Transactional
    public UserDto register(String name, String email, String password) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (email.isEmpty() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            throw new IllegalArgumentException("A valid email is required");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserEmailExistsException("This email already exists");
        }
        validatePassword(password);

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(password);

        Account account = new Account();
        account.setCreatedAt(LocalDateTime.now());

        user.attachNewAccount(account);

        return userMapper.toDto(userRepository.save(user));
    }

    /**
     *Validates password
     *
     * @param email User existing email
     * @param password User existing password
     * @return userDto
     * @exception UserNotFoundException if user email is not found
     * @exception InvalidCredentialsException if user email is not correct
     */
    public UserDto login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));

        if (!user.getPasswordHash().equals(password)) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return userMapper.toDto(user);
    }

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
    @Transactional
    public UserDto changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found by id: " + userId));

        if (!user.getPasswordHash().equals(oldPassword)) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (oldPassword.equals(newPassword)) {
            throw new InvalidCredentialsException("New password must be different from current password");
        }

        validatePassword(newPassword);

        user.setPasswordHash(newPassword);

        return userMapper.toDto(userRepository.save(user));
    }


    //-----------lookup-----------

    /**
     *Validates password strength against local policy.
     *
     * @param password raw password to validate
     * @throws WeakPasswordException if any rule is violated
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Password must contain at least one digit");
        }
    }

    /**
     * Fetches a user by id or throws if not found.
     *
     * @param userId the account identifier
     * @return the account entity
     * @throws UserNotFoundException if User is not found by id
     */
    @Transactional
    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found by id: " + userId));
    }
}
