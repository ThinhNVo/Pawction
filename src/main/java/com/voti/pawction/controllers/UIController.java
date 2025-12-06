package com.voti.pawction.controllers;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.dtos.response.UserDto;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.AccountExceptions.AccountNotFoundException;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.SearchExceptions.EmptySearchException;
import com.voti.pawction.exceptions.SearchExceptions.SearchLengthException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.UserMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.services.auction.AuctionService;
import com.voti.pawction.services.auction.BiddingService;
import com.voti.pawction.services.pet.PetService;
import com.voti.pawction.services.user.UserService;
import com.voti.pawction.services.wallet.AccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

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
    private final AccountService accountService;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHomePage(HttpServletResponse response, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {

            UserDto user = (UserDto) session.getAttribute("loggedInUser");

            List<Map<String, Object>> products = user != null
                    ? auctionService.getLiveAuctions(user.getUserId(), null)
                    : auctionService.getLiveAuctions(null, null);

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

        model.addAttribute("pageText", "View All Results");
        model.addAttribute("pageTitle", "Pawction - Home");
        model.addAttribute("bannerImage", "/images/banner.png");

        if (!isLoggedIn(session)) {
            model.addAttribute("loggedIn", false);
            return "index";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");
        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);
        return "index";
    }

    @GetMapping("/home/category/{type}")
    public String showCategoryPage(@PathVariable Category type,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            UserDto user = (UserDto) session.getAttribute("loggedInUser");

            // Pass userId for filtering out their own auctions
            Long userId = (user != null) ? user.getUserId() : null;

            List<Map<String, Object>> products = auctionService.getLiveAuctions(userId, type);

            for (Map<String, Object> product : products) {
                Long auctionId = (Long) product.get("auctionId");
                int bidCount = biddingService.getBidCountForAuction(auctionId);
                product.put("bidCount", bidCount);
            }

            model.addAttribute("products", products);


            model.addAttribute("category", type);
        } catch (Exception ex) {
            model.addAttribute("products", List.of());
            model.addAttribute("errorMessage", "Could not load category auctions.");
        }

        switch (type) {
            case Cat -> {
                model.addAttribute("pageText", "View Cat Auctions");
                model.addAttribute("pageTitle", "Pawction - Cat Auctions");
                model.addAttribute("bannerImage", "/images/cat_banner.png");
                model.addAttribute("bannerAlt", "Cat Auctions Banner");
            }
            case Dog -> {
                model.addAttribute("pageText", "View Dog Auctions");
                model.addAttribute("pageTitle", "Pawction - Dog Auctions");

                model.addAttribute("bannerImage", "/images/dog_banner.png");
                model.addAttribute("bannerAlt", "Dog Auctions Banner");
            }
            default -> {
                model.addAttribute("pageText", "View All Results");
                model.addAttribute("pageTitle", "Pawction - Home");
                model.addAttribute("bannerImage", "/images/banner.png");
                model.addAttribute("bannerAlt", "Pawction Home Banner");
            }
        }

        if (!isLoggedIn(session)) {
            model.addAttribute("loggedIn", false);
            return "index";
        }

        model.addAttribute("loggedIn", true);
        model.addAttribute("user", session.getAttribute("loggedInUser"));

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
                model.addAttribute("user", user);

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

    @GetMapping("/product/{auctionId}/bids")
    public String showBidHistory(@PathVariable Long auctionId,
                                 Model model,
                                 RedirectAttributes redirectAttributes, HttpSession session) {
        try {

            if (!isLoggedIn(session)) {
                model.addAttribute("loggedIn", false);
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to view bid history.");
                return "redirect:/login";
            }

            UserDto user = (UserDto) session.getAttribute("loggedInUser");
            model.addAttribute("loggedIn", true);
            model.addAttribute("user", user);


            AuctionDto auction = auctionService.getAuctionDto(auctionId);

            boolean hasBid = biddingService.hasUserBidOnAuction(user.getUserId(), auctionId);

            if (!hasBid) {
                redirectAttributes.addFlashAttribute("errorMessage", "You must have placed a bid to view bid history.");
                return "redirect:/product/" + auctionId;
            }

            List<BidDto> bids = biddingService.getAllBidsForAuction(auction.getAuctionId());
            PetDto pet = petService.getPetDtoOrThrow(auction.getPetId());

            Map<Long, UserDto> bidders = new HashMap<>();
            for (BidDto bid : bids) {
                UserDto bidder = userService.getUserOrThrow(bid.getBidderId());
                bidders.put(bid.getBidId(), bidder);
            }

            Optional<BidDto> winningBidOpt = biddingService.getWinningBid(auctionId);
            String winningBidderName = winningBidOpt
                    .map(bid -> userService.getUserOrThrow(bid.getBidderId()).getName())
                    .orElse("None");

            model.addAttribute("auction", auction);
            model.addAttribute("pet", pet);
            model.addAttribute("bids", bids);
            model.addAttribute("bidders", bidders);
            model.addAttribute("winningBidderName", winningBidderName);

            return "bid_list";
        } catch (AuctionNotFoundException | PetNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Auction not found.");
            return "redirect:/home";
        }
    }

    @GetMapping("/account")
    public String showAccountPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to view your account.");
            return "redirect:/login";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");
        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);

        try {
            // Fetch balances
            BigDecimal balance = accountService.getBalance(user.getUserId());
            BigDecimal available = accountService.getAvailable(user.getUserId());

            model.addAttribute("balance", balance);
            model.addAttribute("availableBalance", available);
        } catch (AccountNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found: " + ex.getMessage());
            return "redirect:/home";
        }

        // Auctions created by this user
        List<Map<String, Object>> myAuctions = auctionService.getAuctionsByUser(user.getUserId());
        // Auctions this user has bid on
        List<Map<String, Object>> auctionsBiddedOn = biddingService.getAuctionsUserHasBiddedOn(user.getUserId());

        for (Map<String, Object> product : myAuctions) {
            Long auctionId = (Long) product.get("auctionId");
            int bidCount = biddingService.getBidCountForAuction(auctionId);
            product.put("bidCount", bidCount);
        }
        for (Map<String, Object> product : auctionsBiddedOn) {
            Long auctionId = (Long) product.get("auctionId");
            int bidCount = biddingService.getBidCountForAuction(auctionId);
            product.put("bidCount", bidCount);
        }

        model.addAttribute("myAuctions", myAuctions);
        model.addAttribute("auctionsIBiddedOn", auctionsBiddedOn);

        return "account";
    }

    @GetMapping("/search")
    public String showSearchPage(@RequestParam("breed") String breed,
                                 HttpSession session,
                                 Model model) {

        try {
            if (breed == null || breed.trim().isEmpty()) {
                throw new EmptySearchException("Search term cannot be empty");
            }

            String normalized = breed.trim().replaceAll("\\s+", "");
            if (normalized.length() < 3) {
                throw new SearchLengthException("Search term must be at least 3 letters long");
            }

            UserDto user = (UserDto) session.getAttribute("loggedInUser");
            Long userId = (user != null) ? user.getUserId() : null;

            List<Map<String, Object>> products = auctionService.getLiveAuctionsByBreed(userId, normalized);

            for (Map<String, Object> product : products) {
                Long auctionId = (Long) product.get("auctionId");
                int bidCount = biddingService.getBidCountForAuction(auctionId);
                product.put("bidCount", bidCount);
            }

            model.addAttribute("products", products);
            model.addAttribute("searchTerm", breed);
            model.addAttribute("pageTitle", "Search Results for " + breed);

        } catch (SearchLengthException | EmptySearchException ex) {
            model.addAttribute("products", List.of());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Search");

        } catch (Exception ex) {
            model.addAttribute("products", List.of());
            model.addAttribute("errorMessage", "Could not process search: " + breed);
            model.addAttribute("pageTitle", "Search");
        }

        boolean loggedIn = isLoggedIn(session);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("user", session.getAttribute("loggedInUser"));
        }

        return "search_page";
    }

    @PostMapping("/account/{userId}/funds")
    public String manageFunds(@PathVariable Long userId,
                              @RequestParam BigDecimal amount,
                              @RequestParam String actionType,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to manage funds.");
            return "redirect:/login";
        }

        UserDto user = (UserDto) session.getAttribute("loggedInUser");
        if (user == null || !user.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized account access.");
            return "redirect:/login";
        }

        try {
            Account account = accountService.getAccountOrThrow(userId);
            Long accountId = account.getAccountId();

            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
            String formattedAmount = currencyFormatter.format(amount);

            if ("deposit".equalsIgnoreCase(actionType)) {
                accountService.deposit(accountId, amount);
                redirectAttributes.addFlashAttribute("successMessage", "Successfully deposited " + formattedAmount);
            } else if ("withdraw".equalsIgnoreCase(actionType)) {
                accountService.withdraw(accountId, amount);
                redirectAttributes.addFlashAttribute("successMessage", "Successfully withdrew " + formattedAmount);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid action specified.");
            }
        } catch (AccountNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found: " + ex.getMessage());
        } catch (InvalidAmountException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid amount: " + ex.getMessage());
        } catch (NullPointerException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Amount must not be null.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Operation failed: " + ex.getMessage());
        }

        return "redirect:/account";
    }



}
