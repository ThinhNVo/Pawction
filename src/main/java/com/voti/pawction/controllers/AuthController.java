package com.voti.pawction.controllers;

import com.voti.pawction.dtos.request.UserRequest.ChangePasswordRequest;
import com.voti.pawction.dtos.request.UserRequest.LoginRequest;
import com.voti.pawction.dtos.request.UserRequest.RegisterUserRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;
import com.voti.pawction.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        // If already logged in, redirect to users list
     if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/home";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest loginRequest,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            // Call service layer for authentication
            UserDto user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

            // Store user in session
            session.setAttribute("loggedInUser", user);

            return "redirect:/home";

        } catch (UserNotFoundException | InvalidCredentialsException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // If already logged in, redirect to home page
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/home";
        }
        model.addAttribute("registerRequest", new RegisterUserRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterUserRequest registerUserRequest,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            // Call service layer for authentication
            UserDto user = userService.register(registerUserRequest.getName(),
                    registerUserRequest.getEmail(),
                    registerUserRequest.getPassword());

            redirectAttributes.addFlashAttribute("successMessage", "Account Created Successfully! Please log in.");
            return "redirect:/login";

        } catch (WeakPasswordException | InvalidCredentialsException | UserEmailExistsException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/home";
    }

    /**
     * Display change password page
     */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        UserDto loggedInUser = (UserDto) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to continue");
            return "redirect:/login";
        }

        model.addAttribute("user", loggedInUser);
        return "change-password";
    }

    /**
     * Handle change password form submission
     */
    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute ChangePasswordRequest request,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserDto loggedInUser = (UserDto) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to continue");
            return "redirect:/login";
        }

        try {
            // Call service layer for password change
            UserDto updatedUser = userService.changePassword(
                    loggedInUser.getUserId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            // Invalidate session to force re-login
            session.invalidate();

            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
            return "redirect:/login";

        } catch (InvalidCredentialsException | WeakPasswordException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/change-password";
        }
    }
}
