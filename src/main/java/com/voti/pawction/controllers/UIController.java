package com.voti.pawction.controllers;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.UserRequest.LoginRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.mappers.UserMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping()
public class UIController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            model.addAttribute("loggedIn", false);
            return "index";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");

        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);
        return "index";
    }


    @GetMapping ("/auction/add")
    public String showAddAuctionPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to add an auction.");
            return "redirect:/login";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");

        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);
        return "add-auction";
    }

    @PostMapping("/auction/add")
    public String addAuction(@ModelAttribute CreateAuctionRequest auctionRequest,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            auctionService.createAuction(auctionRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Auction created successfully!");
            return "redirect:/home";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create auction: " + e.getMessage());
            return "redirect:/auction/add";
        }

}
