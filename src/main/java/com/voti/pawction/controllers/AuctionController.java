package com.voti.pawction.controllers;


import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.UserRequest.LoginRequest;
import com.voti.pawction.dtos.request.UserRequest.RegisterUserRequest;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;
import com.voti.pawction.services.auction.AuctionService;
import com.voti.pawction.services.pet.PetService;
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

public class AuctionController {
    private final AuctionService auctionService;
    private final UserService userService;
    private final PetService petService;
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }



    @GetMapping("/auction/add")
    public String showAddAuctionPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to add an auction.");
            return "redirect:/login";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");

        model.addAttribute("auctionRequest", new CreateAuctionRequest()); // matches th:object
        model.addAttribute("pets", petService.getPetsByOwner(user.getId())); // populate dropdown
        return "add_auction";
    }

    @PostMapping("/auction/add")
    public String addAuction(@ModelAttribute("auctionRequest") CreateAuctionRequest auctionRequest,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            UserDto seller = (UserDto) session.getAttribute("loggedInUser");
            if (seller == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to add an auction.");
                return "redirect:/login";
            }

         //   .createPetByOwner(user.getId()));

            auctionService.create(seller.getId(), auctionRequest.getPetId(), auctionRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Auction created successfully!");
            return "redirect:/home";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create auction: " + e.getMessage());
            return "redirect:/auction/add";
        }

    }
}
