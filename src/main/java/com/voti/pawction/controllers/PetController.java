package com.voti.pawction.controllers;

import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.pet.Pet;
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
public class PetController {

    private final PetService petService;
    private final UserService userService;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }


    @GetMapping("/pet/addDog")
    public String showAddPetForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to add an auction.");
            return "redirect:/login";
        }

        model.addAttribute("pet", new Pet());
        return "add_dog";
    }





}
