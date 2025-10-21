package com.voti.pawction.services;

import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Allergy;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.entities.pet.enums.Size;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;


    public void createTestUser() {
        var user = User.builder()
                .name("Test User")
                .email("testuser@gmail.com")
                .passwordHash("123456")
                .build();
        userRepository.save(user);
        System.out.println("Test user created: " + user);
    }

    @Transactional
   public void createTestBid()
   {
       var user = User.builder()
               .name("Bidder User")
               .email("bidder@example.com")
               .passwordHash("secure123")
               .build();
       userRepository.save(user);

       var auction = Auction.builder()
               .startPrice(20.0)
               .highestBid(0.0)
               .status(Auction_Status.LIVE)
               .createdAt(LocalDateTime.now())
               .updatedAt(LocalDateTime.now())
               .endTime(LocalDateTime.now())
               .sellingUser(user)

               .build();
       auctionRepository.save(auction);

       var pet = Pet.builder()
               .petName("Barkley")
               .petAgeMonths(18)
               .petSex(Sex.M)
               .petWeight(12.5)
               .petCategory(Category.DOG)
               .dogBreed("Beagle")
               .dogSize(Size.MEDIUM)
               .dogTemperature("Friendly")
               .dogIsHypoallergenic(Allergy.NO)
                .auction(auction)
               .build();
       petRepository.save(pet);
       System.out.println("Sample dog pet created: " + pet);

       var bid = Bid.builder()
               .amount(150.0)
               .bidStatus(Bid_Status.WINNING)
               .bidTime(LocalDateTime.now())
               .user(user)
               .auction(auction)
               .build();

         bidRepository.save(bid);

         auction.setHighestBid(bid.getAmount());
         auction.setWinningUser(user);
         auctionRepository.save(auction);
       System.out.println("Test auction created: " + auction);

       System.out.println("Test bid created: " + bid);

   }

}
