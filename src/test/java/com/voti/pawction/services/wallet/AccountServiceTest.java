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
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
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

    @Autowired private AccountService accountService;
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

        // start tests from a clean ledger balance of 0 (but with an attached account)
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
        auction.setStartPrice(new BigDecimal("20.00"));
        auction.setHighestBid(new BigDecimal("0.00"));
        auction.setDescription("Dog auction");
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction.setSellingUser(us);
        auction.setPet(pet);
        auction = auctionRepository.save(auction);

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
        var ex = assertThrows(InvalidAmountException.class,
                () -> accountService.withdraw(accountId, new BigDecimal("150.00")));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    @Transactional
    @DisplayName("available: equals balance when no holds exist")
    void available_equalsBalance_whenNoHolds() {
        accountService.deposit(accountId, new BigDecimal("40.00"));

        BigDecimal balance = accountService.getBalance(accountId);
        BigDecimal available = accountService.getAvailable(accountId);

        assertThat(balance).isEqualByComparingTo("40.00");
        // business rule: available = balance - SUM(HELD holds); with no holds, it should equal balance
        assertThat(available).isEqualByComparingTo("40.00");
    }

    @Nested
    @DisplayName("holds")
    @Transactional
    class Holds {

        @Test
        @DisplayName("placeHold: keeps ledger balance, reduces available, and creates HELD hold")
        @Transactional
        void placeHold_reducesAvailable_notBalance() {
            // Arrange
            accountService.deposit(accountId, new BigDecimal("100.00"));

            BigDecimal beforeBalance = accountService.getBalance(accountId);
            BigDecimal beforeAvailable = accountService.getAvailable(accountId);

            assertThat(beforeBalance).isEqualByComparingTo("100.00");
            assertThat(beforeAvailable).isEqualByComparingTo("100.00");

            // Act
            DepositHold hold = accountService.placeHold(accountId, auctionId, new BigDecimal("25.00"));

            // Assert
            Account reloaded = accountRepository.findById(accountId).orElseThrow();
            BigDecimal afterBalance = accountService.getBalance(accountId);
            BigDecimal afterAvailable = accountService.getAvailable(accountId);

            // Ledger balance should not change; only availability is reduced
            assertThat(reloaded.getBalance()).isEqualByComparingTo("100.00");
            assertThat(afterBalance).isEqualByComparingTo("100.00");
            assertThat(afterAvailable).isEqualByComparingTo("75.00");

            // Ensure Hold got written on Account and Auction, and is HELD
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
            assertThat(accountHold.getDepositStatus()).isEqualTo(Status.HELD);
            assertThat(hold.getDepositStatus()).isEqualTo(Status.HELD);
        }

        @Test
        @DisplayName("place + release: restores available and marks hold as RELEASED")
        @Transactional
        void placeAndReleaseHold_restoresAvailable() {
            // Arrange
            accountService.deposit(accountId, new BigDecimal("50.00"));

            BigDecimal initialBalance = accountService.getBalance(accountId);
            assertThat(initialBalance).isEqualByComparingTo("50.00");

            // Act
            accountService.placeHold(accountId, auctionId, new BigDecimal("25.00"));
            BigDecimal duringAvailable = accountService.getAvailable(accountId);
            assertThat(duringAvailable).isEqualByComparingTo("25.00");

            accountService.releaseHold(accountId, auctionId);

            // Assert: ledger balance unchanged, available fully restored
            Account reloaded = accountRepository.findById(accountId).orElseThrow();
            BigDecimal finalBalance = accountService.getBalance(accountId);
            BigDecimal finalAvailable = accountService.getAvailable(accountId);

            assertThat(reloaded.getBalance()).isEqualByComparingTo("50.00");
            assertThat(finalBalance).isEqualByComparingTo("50.00");
            assertThat(finalAvailable).isEqualByComparingTo("50.00");

            DepositHold accountHold = accountService.getHolds(accountId).stream()
                    .filter(h -> Objects.equals(h.getAuction().getAuctionId(), auctionId))
                    .findFirst().orElseThrow();
            DepositHold auctionHold = auctionRepository.findById(auctionId).orElseThrow()
                    .getDepositHolds().stream()
                    .filter(h -> Objects.equals(h.getAccount().getAccountId(), accountId))
                    .findFirst().orElseThrow();

            assertThat(accountHold).isEqualTo(auctionHold);
            assertThat(accountHold.getDepositStatus()).isEqualTo(Status.RELEASED);
            assertThat(auctionHold.getDepositStatus()).isEqualTo(Status.RELEASED);

            // No active HELD holds should remain for this account/auction
            boolean hasActiveForAuction = accountService.getActiveHolds(accountId).stream()
                    .anyMatch(h -> Objects.equals(h.getAuction().getAuctionId(), auctionId));
            assertThat(hasActiveForAuction).isFalse();
        }
    }

    @Test
    @DisplayName("validation: negative/zero amounts rejected")
    @Transactional
    void validation_negativeOrZeroAmounts_rejected() {
        assertAll(
                () -> assertThrows(InvalidAmountException.class,
                        () -> accountService.deposit(accountId, new BigDecimal("-1.00"))),
                () -> assertThrows(InvalidAmountException.class,
                        () -> accountService.deposit(accountId, new BigDecimal("0.00"))),
                () -> assertThrows(InvalidAmountException.class,
                        () -> accountService.withdraw(accountId, new BigDecimal("0.00")))
        );
    }
}
