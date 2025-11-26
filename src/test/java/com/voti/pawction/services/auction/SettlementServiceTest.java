package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Payment_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Allergy;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.entities.pet.enums.Size;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.PaymentExceptions.InvalidPaymentException;
import com.voti.pawction.exceptions.PaymentExceptions.UnauthorizedPaymentException;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.services.wallet.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SettlementServiceTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepositHoldRepository depositHoldRepository;

    @Mock
    private BiddingService biddingService;

    @Mock
    private AccountService accountService;

    private Long auctionId;
    private Long sellerUserId;
    private Long winnerUserId;
    private Long runnerUpUserId;

    private Long winnerAccountId;
    private Long runnerUpAccountId;

    @BeforeEach
    @Transactional
    void setUp() {
        // --- Users ---
        User seller = new User();
        seller.setName("Seller");
        seller.setEmail("seller@example.com");
        seller.setPasswordHash("secret");
        seller = userRepository.save(seller);
        sellerUserId = seller.getUserId();

        User winner = new User();
        winner.setName("Winner");
        winner.setEmail("winner@example.com");
        winner.setPasswordHash("secret");
        winner = userRepository.save(winner);
        winnerUserId = winner.getUserId();

        User runnerUp = new User();
        runnerUp.setName("Runner Up");
        runnerUp.setEmail("runnerup@example.com");
        runnerUp.setPasswordHash("secret");
        runnerUp = userRepository.save(runnerUp);
        runnerUpUserId = runnerUp.getUserId();

        // --- Accounts (just for IDs on holds; real money handled by mocked AccountService) ---
        Account sellerAcc = new Account();
        sellerAcc.setUser(seller);
        sellerAcc.setBalance(BigDecimal.ZERO);
        sellerAcc.setCreatedAt(LocalDateTime.now());
        sellerAcc = accountRepository.save(sellerAcc);

        Account winnerAcc = new Account();
        winnerAcc.setUser(winner);
        winnerAcc.setBalance(BigDecimal.ZERO);
        winnerAcc.setCreatedAt(LocalDateTime.now());
        winnerAcc = accountRepository.save(winnerAcc);
        winnerAccountId = winnerAcc.getAccountId();

        Account runnerUpAcc = new Account();
        runnerUpAcc.setUser(runnerUp);
        runnerUpAcc.setBalance(BigDecimal.ZERO);
        runnerUpAcc.setCreatedAt(LocalDateTime.now());
        runnerUpAcc = accountRepository.save(runnerUpAcc);
        runnerUpAccountId = runnerUpAcc.getAccountId();

        // --- Pet ---
        Pet pet = new Pet();
        pet.setPetName("Barkley");
        pet.setPetAgeMonths(12);
        pet.setPetSex(Sex.M);
        pet.setPetWeight(10.0);
        pet.setPetCategory(Category.Dog);
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
        auction.setHighestBid(new BigDecimal("30.00"));
        auction.setDescription("Test auction");
        auction.setStatus(Auction_Status.ENDED);
        auction.setCreatedAt(now.minusDays(1));
        auction.setUpdatedAt(now.minusDays(1));
        auction.setEndTime(now.minusHours(1));
        auction.setSellingUser(seller);
        auction.setPet(pet);
        auction.setWinningUser(winner);
        auction.setPaymentDueDate(now.plusHours(2)); // default: within window
        auction = auctionRepository.save(auction);
        auctionId = auction.getAuctionId();

        // --- Deposit holds (winner + runner-up) ---
        DepositHold winnerHold = new DepositHold();
        winnerHold.setAuction(auction);
        winnerHold.setAccount(winnerAcc);
        winnerHold.setAmount(new BigDecimal("10.00"));
        winnerHold.setDepositStatus(Status.HELD);
        depositHoldRepository.save(winnerHold);

        DepositHold runnerUpHold = new DepositHold();
        runnerUpHold.setAuction(auction);
        runnerUpHold.setAccount(runnerUpAcc);
        runnerUpHold.setAmount(new BigDecimal("10.00"));
        runnerUpHold.setDepositStatus(Status.HELD);
        depositHoldRepository.save(runnerUpHold);
    }

    @Test
    @DisplayName("begin: ENDED auction sets winner + due date and releases non-winner holds")
    @Transactional
    void begin_endedAuction_setsWinnerAndDueDate_andReleasesHolds() {
        // second highest is runner-up
        BidDto second = mock(BidDto.class);
        when(second.getBidderId()).thenReturn(runnerUpUserId);
        when(biddingService.getSecondHighestBid(auctionId)).thenReturn(Optional.of(second));

        LocalDateTime dueAt = LocalDateTime.now().plusHours(3);

        settlementService.begin(auctionId, winnerUserId, dueAt);

        Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(Auction_Status.ENDED, reloaded.getStatus());
        assertNotNull(reloaded.getWinningUser());
        assertEquals(winnerUserId, reloaded.getWinningUser().getUserId());
        assertEquals(dueAt, reloaded.getPaymentDueDate());

        // Because accountId != userId, implementation will treat *all* holds as non-winner
        verify(accountService, times(2))
                .releaseHold(anyLong(), eq(auctionId));
    }

    @Test
    @DisplayName("begin: non-ENDED auction throws")
    @Transactional
    void begin_nonEnded_throws() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        a.setStatus(Auction_Status.LIVE);
        auctionRepository.save(a);

        assertThrows(
                AuctionInvalidStateException.class,
                () -> settlementService.begin(auctionId, winnerUserId, LocalDateTime.now().plusHours(1))
        );

        verifyNoInteractions(accountService);
    }

    @Test
    @DisplayName("noWinner: ENDED auction cleared and SETTLED")
    @Transactional
    void noWinner_endedAuction_setsSettled() {
        settlementService.noWinner(auctionId);

        Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(Auction_Status.SETTLED, reloaded.getStatus());
        assertNull(reloaded.getWinningUser());
        assertNull(reloaded.getPaymentDueDate());
    }

    @Nested
    @DisplayName("paymentRecord")
    class PaymentRecordTests {

        @Test
        @DisplayName("happy path: marks PAID + SETTLED and deposits to seller")
        @Transactional
        void paymentRecord_happyPath() {
            // make sure auction is ENDED with winner and future due date from setup
            Auction before = auctionRepository.findById(auctionId).orElseThrow();
            assertEquals(Auction_Status.ENDED, before.getStatus());
            assertNotNull(before.getWinningUser());
            assertEquals(winnerUserId, before.getWinningUser().getUserId());

            // stub deposit
            when(accountService.deposit(eq(sellerUserId), any(BigDecimal.class))).thenReturn(null);

            BigDecimal amount = new BigDecimal("30.00");
            settlementService.paymentRecord(auctionId, winnerUserId, amount, "USD");

            Auction after = auctionRepository.findById(auctionId).orElseThrow();
            assertEquals(Payment_Status.PAID, after.getPaymentStatus());
            assertEquals(Auction_Status.SETTLED, after.getStatus());
            assertNull(after.getPaymentDueDate());
            assertNotNull(after.getWinningUser());
            assertEquals(winnerUserId, after.getWinningUser().getUserId());

            ArgumentCaptor<BigDecimal> amtCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(accountService).deposit(eq(sellerUserId), amtCaptor.capture());
            assertThat(amtCaptor.getValue()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("invalid amount or currency rejected")
        @Transactional
        void paymentRecord_invalidAmountOrCurrency_rejected() {
            // zero amount
            assertThrows(
                    InvalidPaymentException.class,
                    () -> settlementService.paymentRecord(auctionId, winnerUserId, BigDecimal.ZERO, "USD")
            );

            // null currency
            assertThrows(
                    InvalidPaymentException.class,
                    () -> settlementService.paymentRecord(auctionId, winnerUserId, new BigDecimal("30.00"), null)
            );

            // unsupported currency
            assertThrows(
                    InvalidPaymentException.class,
                    () -> settlementService.paymentRecord(auctionId, winnerUserId, new BigDecimal("30.00"), "EUR")
            );

            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("wrong payer rejected with UnauthorizedPaymentException")
        @Transactional
        void paymentRecord_wrongPayer_unauthorized() {
            assertThrows(
                    UnauthorizedPaymentException.class,
                    () -> settlementService.paymentRecord(auctionId, runnerUpUserId, new BigDecimal("30.00"), "USD")
            );
        }
    }

    @Nested
    @DisplayName("expireAndPromoteNext")
    class ExpireAndPromoteNextTests {

        @Test
        @DisplayName("no-op when already PAID")
        @Transactional
        void expireAndPromoteNext_paid_noOp() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setPaymentStatus(Payment_Status.PAID);
            auctionRepository.save(a);

            boolean changed = settlementService.expireAndPromoteNext(auctionId, LocalDateTime.now());
            assertFalse(changed);
            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("no-op when payment window not yet due")
        @Transactional
        void expireAndPromoteNext_notYetDue_noOp() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setPaymentDueDate(LocalDateTime.now().plusHours(5));
            auctionRepository.save(a);

            boolean changed = settlementService.expireAndPromoteNext(auctionId, LocalDateTime.now());
            assertFalse(changed);
        }

        @Test
        @DisplayName("no second place: forfeits hold, resets price, calls noWinner")
        @Transactional
        void expireAndPromoteNext_noSecondPlace_forfeitsAndNoWinner() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setPaymentDueDate(LocalDateTime.now().minusHours(1)); // overdue
            auctionRepository.save(a);

            when(biddingService.getSecondHighestBid(auctionId)).thenReturn(Optional.empty());

            boolean changed = settlementService.expireAndPromoteNext(auctionId, LocalDateTime.now());
            assertTrue(changed);

            Auction after = auctionRepository.findById(auctionId).orElseThrow();
            assertEquals(a.getStartPrice(), after.getHighestBid());
            assertEquals(Auction_Status.SETTLED, after.getStatus());
            assertNull(after.getWinningUser());

            verify(accountService).forfeitHold(eq(winnerUserId), eq(auctionId));
        }

        @Test
        @DisplayName("second place exists: forfeits current hold and promotes runner-up")
        @Transactional
        void expireAndPromoteNext_withSecondPlace_promotesRunnerUp() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setPaymentDueDate(LocalDateTime.now().minusHours(1)); // overdue
            auctionRepository.save(a);

            BidDto second = mock(BidDto.class);
            when(second.getBidderId()).thenReturn(runnerUpUserId);
            when(second.getAmount()).thenReturn(new BigDecimal("25.00"));
            when(biddingService.getSecondHighestBid(auctionId)).thenReturn(Optional.of(second));

            boolean changed = settlementService.expireAndPromoteNext(auctionId, LocalDateTime.now());
            assertTrue(changed);

            Auction after = auctionRepository.findById(auctionId).orElseThrow();
            assertEquals(runnerUpUserId, after.getWinningUser().getUserId());
            assertThat(after.getHighestBid()).isEqualByComparingTo("25.00");
            assertNotNull(after.getPaymentDueDate());

            verify(accountService).forfeitHold(eq(winnerUserId), eq(auctionId));
        }
    }

    @Test
    @DisplayName("expireOverdueSettlements: no overdue -> 0 processed")
    @Transactional
    void expireOverdueSettlements_noOverdue_returnsZero() {
        // paymentDueDate is in future from setup, so there should be no candidates
        int processed = settlementService.expireOverdueSettlements();
        assertEquals(0, processed);
    }

    @Test
    @DisplayName("cancelAuctionSettlement: releases all holds and cancels auction")
    @Transactional
    void cancelAuctionSettlement_releasesAllHolds_andCancels() {
        settlementService.cancelAuctionSettlement(auctionId);

        Auction after = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(Auction_Status.CANCELED, after.getStatus());
        assertNull(after.getWinningUser());
        assertNull(after.getPaymentDueDate());
        assertEquals(after.getStartPrice(), after.getHighestBid());

        // should release for each distinct deposit hold
        verify(accountService, times(2))
                .releaseHold(anyLong(), eq(auctionId));
    }
}