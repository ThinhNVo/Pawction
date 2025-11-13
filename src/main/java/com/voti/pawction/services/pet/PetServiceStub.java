package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.CreatePetRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Allergy;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.entities.pet.enums.Size;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.repositories.wallet.TransactionRepository;
import com.voti.pawction.services.wallet.AccountServiceStub;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PetServiceStub {
    private final UserRepository userRepository;
    private final PetMapper petMapper;
    private final PetRepository petRepository;
    private final AuctionRepository auctionRepository;
    private final AccountRepository accountRepository;


//    @Transactional
    //Create pet and validate it
//    public PetDto registerPet(Long userId, CreatePetRequest request) {
//
//
//    }

    @Transactional
    public void createAuction() {
        System.out.println("=== overallProcess START ===");
        var user = User.builder()
                .name("Test User")
                .email("Test@example.com")
                .passwordHash("secure123")
                .build();
        userRepository.save(user);
        System.out.printf("User saved: id=%s, name=%s, email=%s%n",
                user.getUserId(), user.getName(), user.getEmail());


        var account = Account.builder()
                .balance(BigDecimal.valueOf(0))
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        accountRepository.save(account);

        System.out.printf("Account saved: id=%s, userId=%s, balance=%s%n",
                account.getAccountId(), account.getUser().getUserId(), account.getBalance());


        var pet = Pet.builder()
                .petName("Barkley")
                .petAgeMonths(18)
                .petSex(Sex.M)
                .petWeight(12.5)
                .petCategory(Category.Dog)
                .dogBreed("Beagle")
                .dogSize(Size.MEDIUM)
                .dogTemperament("Friendly")
                .dogIsHypoallergenic(Allergy.UNKNOWN)
                .primaryPhotoUrl("notfound")
                .build();

        System.out.printf("Pet saved: id=%s, name=%s, category=%s%n",
                pet.getPetId(), pet.getPetName(), pet.getPetCategory());

        var auction = Auction.builder()
                .startPrice(BigDecimal.valueOf(15))
                .highestBid(BigDecimal.valueOf(0))
                .status(Auction_Status.LIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .sellingUser(user)
                .pet(pet)
                .build();

        auctionRepository.save(auction);

        System.out.printf("Auction saved: id=%s, status=%s, start=%.2f, petId=%s, sellerId=%s%n",
                auction.getAuctionId(), auction.getStatus(), auction.getStartPrice(),
                auction.getPet().getPetId(), auction.getSellingUser().getUserId());

    }
}
