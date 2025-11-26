package com.voti.pawction.controllers;


import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.RegisterPetAndAuctionRequest;
import com.voti.pawction.dtos.request.UserRequest.LoginRequest;
import com.voti.pawction.dtos.request.UserRequest.RegisterUserRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.InvalidAuctionException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.exceptions.UserExceptions.InvalidCredentialsException;
import com.voti.pawction.exceptions.UserExceptions.UserEmailExistsException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.WeakPasswordException;
import com.voti.pawction.services.auction.AuctionService;
import com.voti.pawction.services.pet.PetService;
import com.voti.pawction.services.storage.FileStorageService;
import com.voti.pawction.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;

@Controller
@AllArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final UserService userService;
    private final PetService petService;
    private final FileStorageService fileStorageService;

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

        model.addAttribute("registerPetAndAuctionRequest", new RegisterPetAndAuctionRequest()); // matches th:object
        return "add_auction";
    }

    @PostMapping("/auction/add")
    public String addAuction(@ModelAttribute RegisterPetAndAuctionRequest request, HttpSession session, RedirectAttributes redirectAttributes){
        UserDto seller = (UserDto) session.getAttribute("loggedInUser");
        if (seller == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to add an auction.");
            return "redirect:/login";
        }

        try {
            // Register the pet first
            PetDto petDto;
            if (request.getPetRequest().getCategory() == Category.Dog) {
                petDto = petService.registerDog(seller.getUserId(), request.getPetRequest().toDogRequest());
            } else if (request.getPetRequest().getCategory() == Category.Cat) {
                petDto = petService.registerCat(seller.getUserId(), request.getPetRequest().toCatRequest());
            } else {
                throw new ValidationException("Unsupported category");
            }

            auctionService.create(seller.getUserId(), petDto.getPetId(), request.getAuctionRequest());

            // Attach photo to pet last, if error occurs photo is not saved
            MultipartFile photo = request.getPetRequest().getPrimaryPhoto();
            if (photo != null && !photo.isEmpty()) {
                String photoUrl = fileStorageService.store(photo);
                petService.attachPhoto(petDto.getPetId(), photoUrl);
            }

            // Then create the auction for the registered pet
            redirectAttributes.addFlashAttribute("successMessage", "Auction created successfully!");
            return "redirect:/auction/add";
        } catch (ValidationException | InvalidAuctionException | InvalidAmountException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auction/add";
        } catch (UserNotFoundException | PetNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Resource not found: " + e.getMessage());
            return "redirect:/auction/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unexpected error: " + e.getMessage());
            return "redirect:/auction/add";
        }

    }
}
