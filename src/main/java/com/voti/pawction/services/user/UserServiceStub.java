package com.voti.pawction.services.user;

import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.wallet.Transaction;
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

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class UserServiceStub {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AccountRepository accountRepository;
    private final AccountServiceStub accService;
    private final DepositHoldRepository depositHoldRepository;
    private final TransactionRepository transactionRepository;


//    public void createTestUser() {
//        var user = User.builder()
//                .name("Test User")
//                .email("testuser@gmail.com")
//                .passwordHash("123456")
//                .build();
//        userRepository.save(user);
//        System.out.println("Test user created: " + user);
//    }
//
//    @Transactional
//   public void createTestBid()
//   {
//       var user = User.builder()
//               .name("Bidder User")
//               .email("bidder@example.com")
//               .passwordHash("secure123")
//               .build();
//       userRepository.save(user);
//
//       var pet = Pet.builder()
//               .petName("Barkley")
//               .petAgeMonths(18)
//               .petSex(Sex.M)
//               .petWeight(12.5)
//               .petCategory(Category.DOG)
//               .dogBreed("Beagle")
//               .dogSize(Size.MEDIUM)
//               .dogTemperament("Friendly")
//               .dogIsHypoallergenic(Allergy.UNKNOWN)
//               .primaryPhotoUrl("notfound")
//               .build();
//
//       petRepository.save(pet);
//       System.out.println("Sample dog pet created: " + pet);
//
//       var auction = Auction.builder()
//               .startPrice(20.0)
//               .highestBid(0.0)
//               .status(Auction_Status.LIVE)
//               .createdAt(LocalDateTime.now())
//               .updatedAt(LocalDateTime.now())
//               .endTime(LocalDateTime.now())
//               .sellingUser(user)
//               .pet(pet)
//               .build();
//
//       auctionRepository.save(auction);
//
//       var bid = Bid.builder()
//               .amount(150.0)
//               .bidStatus(Bid_Status.WINNING)
//               .bidTime(LocalDateTime.now())
//               .user(user)
//               .auction(auction)
//               .build();
//
//         bidRepository.save(bid);
//
//         auction.setHighestBid(bid.getAmount());
//         auction.setWinningUser(user);
//         auctionRepository.save(auction);
//       System.out.println("Test auction created: " + auction);
//
//       System.out.println("Test bid created: " + bid);
//
//   }
   @Transactional
   public void createAuction()
   {
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


   @Transactional
   public void createUserPool () {
       List<User> users = List.of(
               User.builder().name("Alice Seller").email("alice.seller@example.com").passwordHash("secure123").build(),
               User.builder().name("Bob Bidder").email("bob.bidder@example.com").passwordHash("secure123").build(),
               User.builder().name("Carol Viewer").email("carol.viewer@example.com").passwordHash("secure123").build(),
               User.builder().name("Dave PowerUser").email("dave.poweruser@example.com").passwordHash("secure123").build(),
               User.builder().name("Eve Tester").email("eve.tester@example.com").passwordHash("secure123").build()
       );
       userRepository.saveAll(users);
   }

   // User created -> account created -> user made a deposit transaction and account balance go up
   // -> User find an auction -> User join an auction and deposit hold is created with HELD and balance goes down ->
   // User can request list of transaction, list of all active deposit hold on different auction,
   // and view getAvailable for actual remaining spendable funds
   @Transactional
   public void placeBidAsQualifiedUser () throws AccountNotFoundException {
       // create user and account
       var user1 =userRepository.save(User.builder().name("Alice Seller").email("alice.seller@example.com").passwordHash("secure123").build());
       var account1=accountRepository.save(Account.builder().balance(BigDecimal.valueOf(0)).createdAt(LocalDateTime.now()).user(user1).build());

       System.out.println(account1);
       System.out.println(user1);

       //User make a deposit transaction
       System.out.println(accService.deposit(2L, BigDecimal.valueOf(15)));
       System.out.println(accService.getTransactions(2L ));

       //finding auctions
       var auction = auctionRepository.findById(1L).orElseThrow();

       DepositHold holdAcc1 = account1.addHold(auction, BigDecimal.valueOf(15));

       Bid bid1 = Bid.create(user1, auction, 50.00);

       bidRepository.save(bid1);

       auctionRepository.save(auction);

       for (Bid b : auction.getBids()) {
           System.out.println(b);
       }

       for (DepositHold d : auction.getDepositHolds()) {
           System.out.println(d);
       }

   }

   public void  placeBidAsNotQualifiedUser () {
       var user2 =userRepository.save(User.builder().name("Bob Bidder").email("bob.bidder@example.com").passwordHash("secure123").build());
       var account2=accountRepository.save(Account.builder().balance(BigDecimal.valueOf(0)).createdAt(LocalDateTime.now()).user(user2).build());
       Transaction transaction2 = account2.deposit(BigDecimal.valueOf(20));
       var auction = auctionRepository.findById(1L).orElseThrow();


       DepositHold holdAcc2 = account2.addHold(auction, BigDecimal.valueOf(20));
       Bid bid2 = Bid.create(user2, auction, 100.00);
       bidRepository.save(bid2);

       auctionRepository.save(auction);

       for (Bid b : auction.getBids()) {
           System.out.println(b);
       }

       for (DepositHold d : auction.getDepositHolds()) {
           System.out.println(d);
       }
   }
}
