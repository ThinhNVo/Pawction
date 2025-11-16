package com.voti.pawction.controllers;

import com.voti.pawction.dtos.request.LoginRequest;
import com.voti.pawction.dtos.request.RegisterUserRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.exceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserNotFoundException;
import com.voti.pawction.exceptions.WeakPasswordException;
import com.voti.pawction.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@Slf4j
public class UserRequestController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        // If already logged in, redirect to users list
     if (session.getAttribute("loggedInUser") != null) {
         log.info("User already logged in. Redirecting to /home.");
            return "redirect:/home";
        }
        log.debug("Rendering login page.");
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }
    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest loginRequest,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            // Call service layer for authentication
            UserDto user = userService.Login(loginRequest.getEmail(), loginRequest.getPassword());

            // Store user in session
            session.setAttribute("loggedInUser", user);

            log.info("User '{}' successfully logged in.", user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, " + user.getName() + "!");
            return "redirect:/home";

        } catch (UserNotFoundException | InvalidCredentialsException e) {
            log.warn("Failed login attempt for email '{}': {}", loginRequest.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // If already logged in, redirect to home page
        if (session.getAttribute("loggedInUser") != null) {
            log.info("Logged-in user attempted to access registration page. Redirecting to /home.");
            return "redirect:/home";
        }
        log.debug("Rendering registration page.");
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

            log.info("New user '{}' registered successfully.", user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Account Created Successfully! Please log in.");
            return "redirect:/login";

        } catch (WeakPasswordException | InvalidCredentialsException | UserEmailExistsException e) {
            log.warn("Failed registration for email '{}': {}", registerUserRequest.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Object loggedIn = session.getAttribute("loggedInUser");
        if (loggedIn != null) {
            log.info("User '{}' logged out.", ((UserDto) loggedIn).getEmail());
        } else {
            log.debug("Logout called with no active session.");
        }

        session.invalidate();
        return "redirect:/home";
    }
    // need home page now
}
