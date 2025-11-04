package com.voti.pawction.services.wallet;

import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Allergy;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.entities.pet.enums.Size;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.wallet.Transaction;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.repositories.wallet.TransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccountServiceTest {

    @Autowired private AccountServiceStub accountService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private DepositHoldRepository holdRepository;

    private Long userId;
    private Long accountId;
    private Long auctionId;

    @BeforeEach
    @Transactional
    void setUp() {
        User u = new User();
        u.setName("Test User");
        u.setEmail("test.user@example.com");
        u.setPasswordHash("secret");
        u = userRepository.save(u);

        this.userId = u.getUserId();

        Account a = new Account();
        a.setBalance(new BigDecimal("100.00"));
        a.setCreatedAt(LocalDateTime.now());
        a.setUser(u);
        a = accountRepository.save(a);

        this.accountId = a.getAccountId();

        assertNotNull(accountId);
        assertThat(a.getBalance()).isEqualByComparingTo("100.00");
        a.setBalance(BigDecimal.ZERO);
        accountRepository.save(a);
        transactionRepository.deleteAll();
    }


    @BeforeEach
    @Transactional
    void setUpAuction() {
        //create selling user
        User us = new User();
        us.setName("Test Auction");
        us.setEmail("test.auction@example.com");
        us.setPasswordHash("secret");
        us = userRepository.save(us);

        // --- Seller account ---
        Account account = new Account();
        account.setBalance(new BigDecimal("0.00"));
        account.setCreatedAt(LocalDateTime.now());
        account.setUser(us);
        account = accountRepository.save(account);

        // --- Pet (persist before attaching to auction) ---
        Pet pet = new Pet();
        pet.setPetName("Barkley");
        pet.setPetAgeMonths(18);
        pet.setPetSex(Sex.M);
        pet.setPetWeight(12.5);
        pet.setPetCategory(Category.Dog);          // important: enum NAME, not "Dog"
        pet.setDogBreed("Beagle");
        pet.setDogSize(Size.MEDIUM);
        pet.setDogTemperament("Friendly");
        pet.setDogIsHypoallergenic(Allergy.UNKNOWN);
        pet.setPrimaryPhotoUrl("notfound");
        pet = petRepository.save(pet);

        // --- Auction ---
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction();
        // If your entity uses Double, replace with 20.0 / 0.0
        auction.setStartPrice(new BigDecimal("20.00"));
        auction.setHighestBid(new BigDecimal("0.00"));
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction.setSellingUser(us);
        auction.setPet(pet);
        auction = auctionRepository.save(auction);

        // stash IDs for tests
        //this.sellerAccountId = account.getAccountId();
        //this.petId = pet.getPetId();
        this.auctionId = auction.getAuctionId();
    }

    @Test
    @DisplayName("deposit: increases balance and records a transaction")
    @Transactional
    void deposit_increasesBalance_andCreatesTransaction() {
        // Act
        Transaction tx = accountService.deposit(accountId, new BigDecimal("15.00"));

        // Assert (reload to ensure persistence)
        Account reloaded = accountRepository.findById(accountId).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("15.00");

        List<Transaction> txs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(accountId);
        assertThat(txs).isNotEmpty();
        assertThat(tx.getAmount()).isEqualByComparingTo("15.00");
        assertThat(tx.getAccount().getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("withdraw: decreases balance and records a transaction")
    @Transactional
    void withdraw_decreasesBalance_andCreatesTransaction() {
        // Arrange
        accountService.deposit(accountId, new BigDecimal("20.00"));

        // Act
        Transaction tx = accountService.withdraw(accountId, new BigDecimal("7.50"));

        // Assert
        Account reloaded = accountRepository.findById(accountId).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("12.50");
        assertThat(tx.getAmount()).isEqualByComparingTo("7.50");
    }

    @Test
    @Transactional
    @DisplayName("withdraw: insufficient funds throws")
    void withdraw_insufficientFunds_throws() {
        // No seed deposit
        var ex = assertThrows(IllegalStateException.class,
                () -> accountService.withdraw(accountId, new BigDecimal("150.00")));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Nested
    @DisplayName("holds")
    @Transactional
    class Holds {
        @Test
        @DisplayName("place + capture: reduces final balance")
        @Transactional
        void placeAndCaptureHold() {
            // Arrange
            accountService.deposit(accountId, new BigDecimal("100.00"));

            // Act
            DepositHold holdId = accountService.placeHold(accountId, auctionId, new BigDecimal("25.00"));

            // Assert
            Account reloaded = accountRepository.findById(accountId).orElseThrow();
            assertThat(reloaded.getBalance()).isEqualByComparingTo("75.00");

            // Ensure a transaction got written (implementation-dependent)
            List<Transaction> txs = transactionRepository.findByAccountAccountIdOrderByCreatedAtDesc(accountId);
            assertThat(txs.stream().anyMatch(t -> t.getAmount().compareTo(new BigDecimal("100.00")) == 0)).isTrue();

            // Ensure Hold got written on Auction
            DepositHold accountHold = accountService.getActiveHolds(accountId).stream()
                            .filter(h -> Objects.equals(h.getAuction().getAuctionId(), auctionId))
                            .findFirst().orElseThrow();
            DepositHold auctionHold = auctionRepository.findById(auctionId).orElseThrow()
                            .getDepositHolds().stream()
                            .filter(h -> Objects.equals(h.getAccount().getAccountId(), accountId))
                            .findFirst().orElseThrow();
            assertThat(accountHold).isEqualTo(auctionHold);

            assertThat(accountHold.getAmount()).isEqualByComparingTo("25.00");
            assertThat(auctionHold.getAmount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("place + release: restores available (no net change after release)")
        @Transactional
        void placeAndReleaseHold() {
            // Arrange
            accountService.deposit(accountId, new BigDecimal("50.00"));

            // Act
            accountService.placeHold(accountId, auctionId, new BigDecimal("25.00"));
            accountService.releaseHold(accountId, auctionId);

            // Assert (balance returns to original)
            Account reloaded = accountRepository.findById(accountId).orElseThrow();
            assertThat(reloaded.getBalance()).isEqualByComparingTo("50.00");

            DepositHold accountHold = accountService.getHolds(accountId).stream()
                    .filter(h -> Objects.equals(h.getAuction().getAuctionId(), auctionId))
                    .findFirst().orElseThrow();
            DepositHold auctionHold = auctionRepository.findById(auctionId).orElseThrow()
                    .getDepositHolds().stream()
                    .filter(h -> Objects.equals(h.getAccount().getAccountId(), accountId))
                    .findFirst().orElseThrow();

            assertThat(accountHold).isEqualTo(auctionHold);
            assertThat(accountHold.getDepositStatus()).isEqualTo(auctionHold.getDepositStatus());
        }
    }

    @Test
    @DisplayName("validation: negative/zero amounts rejected")
    @Transactional
    void validation_negativeOrZeroAmounts_rejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> accountService.deposit(accountId, new BigDecimal("-1.00"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> accountService.deposit(accountId, new BigDecimal("0.00"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> accountService.withdraw(accountId, new BigDecimal("0.00")))
                );

    }
}
