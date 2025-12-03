package com.voti.pawction.controllers;

import com.voti.pawction.dtos.request.UserRequest.LoginRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.UserMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.services.auction.AuctionService;
import com.voti.pawction.services.auction.BiddingService;
import com.voti.pawction.services.pet.PetService;
import com.voti.pawction.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
@RequestMapping()
public class UIController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;
    private final AuctionService auctionService;
    private final PetService petService;
    private final BiddingService biddingService;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<Map<String, Object>> products = auctionService.getLiveAuctionsForHomePage();

            // enrich each product with bidCount
            for (Map<String, Object> product : products) {
                Long auctionId = (Long) product.get("auctionId");
                int bidCount = biddingService.getBidCountForAuction(auctionId);
                product.put("bidCount", bidCount);
            }
            model.addAttribute("products", products);
        } catch (PetNotFoundException ex) {
            model.addAttribute("products", List.of());
            model.addAttribute("errorMessage", "Some auctions could not be displayed due to missing data.");
        } catch (Exception ex) {
            model.addAttribute("products", List.of());
            model.addAttribute("errorMessage", "An unexpected error occurred while loading auctions.");
        }

        if (!isLoggedIn(session)) {
            model.addAttribute("loggedIn", false);
            return "index";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");

        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);
        return "index";
    }

    @GetMapping("/product/{auctionId}")
    public String showProductPage(@PathVariable Long auctionId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            AuctionDto auction = auctionService.getAuctionDto(auctionId);
            UserDto seller = userService.getUserOrThrow(auction.getSellingUserId());
            PetDto pet = petService.getPetDtoOrThrow(auction.getPetId());

            model.addAttribute("auction", auction);
            model.addAttribute("seller", seller);
            model.addAttribute("pet", pet);

            UserDto user = (UserDto) session.getAttribute("loggedInUser");

            int bidCount = biddingService.getBidCountForAuction(auctionId);
            model.addAttribute("bidCount", bidCount);

            if (user == null) {
                // not logged in
                model.addAttribute("loggedIn", false);
                model.addAttribute("userHasBid", false);
                model.addAttribute("isAuctionOwner", false);
            } else {
                model.addAttribute("loggedIn", true);

                // check if this user is the auction owner
                boolean isAuctionOwner = user.getUserId().equals(auction.getSellingUserId());
                model.addAttribute("isAuctionOwner", isAuctionOwner);

                BigDecimal minNextBidAmount = auctionService.nextMinimumBid(auctionId);
                model.addAttribute("minNextBidAmount", minNextBidAmount);
                // only allow bid lookup if not the owner
                if (!isAuctionOwner) {
                    BidDto highestBid = biddingService.getUsersHighestBidForAuction(user.getUserId(), auctionId);
                    if (highestBid != null) {
                        model.addAttribute("userHasBid", true);
                        model.addAttribute("userBidAmount", highestBid.getAmount());
                    } else {
                        model.addAttribute("userHasBid", false);
                    }
                } else {
                    model.addAttribute("userHasBid", false);
                }
            }
            return "product_view";
        } catch (AuctionNotFoundException | PetNotFoundException | UserNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load the requested product.");
            return "redirect:/home";
        }
    }
}
