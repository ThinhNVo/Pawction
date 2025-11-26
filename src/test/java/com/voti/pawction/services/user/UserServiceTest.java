package com.voti.pawction.services.user;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;

    private String baseEmail;
    private String strongPassword;

    @BeforeEach
    void setUp() {
        baseEmail = "test.user@example.com";
        strongPassword = "Str0ngPass";
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("register: creates user and attached account")
    void register_createsUserAndAccount() {
        long userCountBefore = userRepository.count();
        long accountCountBefore = accountRepository.count();

        UserDto dto = userService.register("Test User", baseEmail, strongPassword);

        assertNotNull(dto);
        assertNotNull(dto.getUserId());
        assertEquals("Test User", dto.getName());
        assertEquals(baseEmail, dto.getEmail());

        // user persisted
        assertThat(userRepository.count()).isEqualTo(userCountBefore + 1);

        User persisted = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new AssertionError("User not persisted"));

        // account attached via shared primary key
        Optional<Account> accountOpt = accountRepository.findById(persisted.getUserId());
        assertThat(accountOpt).isPresent();
        Account acc = accountOpt.get();
        assertEquals(persisted.getUserId(), acc.getAccountId());
    }

    @Test
    @Transactional
    @DisplayName("register: existing email throws UserEmailExistsException")
    void register_existingEmail_throws() {
        userService.register("User One", baseEmail, strongPassword);

        assertThrows(
                UserEmailExistsException.class,
                () -> userService.register("User Two", baseEmail, strongPassword)
        );
    }

    @Test
    @Transactional
    @DisplayName("register: invalid email format throws IllegalArgumentException")
    void register_invalidEmail_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("User", "not-an-email", strongPassword)
        );
    }

    @Test
    @Transactional
    @DisplayName("register: weak password rejected with WeakPasswordException")
    void register_weakPassword_throws() {
        // too short
        assertThrows(
                WeakPasswordException.class,
                () -> userService.register("User", "weak@example.com", "short")
        );

        // no uppercase
        assertThrows(
                WeakPasswordException.class,
                () -> userService.register("User", "weak2@example.com", "lowercase1")
        );

        // no digit
        assertThrows(
                WeakPasswordException.class,
                () -> userService.register("User", "weak3@example.com", "NoDigitsHere")
        );
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login")
    class LoginTests {

        private Long userId;

        @BeforeEach
        @Transactional
        void createUser() {
            UserDto dto = userService.register("Login User", baseEmail, strongPassword);
            userId = dto.getUserId();
        }

        @Test
        @Transactional
        @DisplayName("login: valid credentials return UserDto")
        void login_success() {
            UserDto loggedIn = userService.login(baseEmail, strongPassword);

            assertNotNull(loggedIn);
            assertEquals(userId, loggedIn.getUserId());
            assertEquals(baseEmail, loggedIn.getEmail());
        }

        @Test
        @Transactional
        @DisplayName("login: non-existent email throws UserNotFoundException")
        void login_missingEmail_throws() {
            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.login("unknown@example.com", strongPassword)
            );
        }

        @Test
        @Transactional
        @DisplayName("login: wrong password throws InvalidCredentialsException")
        void login_wrongPassword_throws() {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.login(baseEmail, "WrongPass1")
            );
        }
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        private Long userId;

        @BeforeEach
        @Transactional
        void createUser() {
            UserDto dto = userService.register("ChangePwd User", baseEmail, strongPassword);
            userId = dto.getUserId();
        }

        @Test
        @Transactional
        @DisplayName("changePassword: correct old password and strong new password updates hash")
        void changePassword_success() {
            String newPassword = "N3wStrongPass";

            UserDto updated = userService.changePassword(userId, strongPassword, newPassword);

            assertNotNull(updated);
            User persisted = userRepository.findById(userId).orElseThrow();
            assertEquals(newPassword, persisted.getPasswordHash());

            // old password should no longer work
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.login(baseEmail, strongPassword)
            );

            // new password should work
            UserDto loggedIn = userService.login(baseEmail, newPassword);
            assertEquals(userId, loggedIn.getUserId());
        }

        @Test
        @Transactional
        @DisplayName("changePassword: wrong old password throws InvalidCredentialsException")
        void changePassword_wrongOld_throws() {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.changePassword(userId, "BadOld1", "Another1Pass")
            );
        }

        @Test
        @Transactional
        @DisplayName("changePassword: new same as old rejected")
        void changePassword_sameAsOld_throws() {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.changePassword(userId, strongPassword, strongPassword)
            );
        }

        @Test
        @Transactional
        @DisplayName("changePassword: weak new password rejected with WeakPasswordException")
        void changePassword_weakNew_throws() {
            assertThrows(
                    WeakPasswordException.class,
                    () -> userService.changePassword(userId, strongPassword, "short")
            );
        }

        @Test
        @Transactional
        @DisplayName("changePassword: missing user id throws UserNotFoundException")
        void changePassword_missingUser_throws() {
            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.changePassword(999999L, "whateverOld1", "NewPass1")
            );
        }
    }
}