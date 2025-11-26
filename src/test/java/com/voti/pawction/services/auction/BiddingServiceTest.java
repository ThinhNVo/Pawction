package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.BidDto;
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
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.BidExceptions.InvalidBidException;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.services.auction.policy.AuctionPolicy;
import com.voti.pawction.services.wallet.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BiddingServiceTest {

    @Autowired private BiddingService biddingService;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private BidRepository bidRepository;
    @Autowired private PetRepository petRepository;

    @Mock private AuctionPolicy auctionPolicy;
    @Mock private AccountService accountService;

    private Long auctionId;
    private Long bidderId;

    private Auction auction;
    private User bidder;

    @BeforeEach
    @Transactional
    void setUp() {
        // --- Seller ---
        User seller = new User();
        seller.setName("Seller");
        seller.setEmail("seller@example.com");
        seller.setPasswordHash("secret");
        seller = userRepository.save(seller);

        // --- Bidder ---
        bidder = new User();
        bidder.setName("Bidder");
        bidder.setEmail("bidder@example.com");
        bidder.setPasswordHash("secret");
        bidder = userRepository.save(bidder);
        this.bidderId = bidder.getUserId();

        // --- Pet (must exist for FK) ---
        Pet pet = new Pet();
        pet.setOwner(seller);
        pet.setPetName("Barkley");
        pet.setPetAgeMonths(18);
        pet.setPetSex(Sex.M);
        pet.setPetWeight(12.5);
        pet.setPetCategory(Category.Dog);
        pet.setDogBreed("Beagle");
        pet.setDogSize(Size.MEDIUM);
        pet.setDogTemperament("Friendly");
        pet.setDogIsHypoallergenic(Allergy.UNKNOWN);
        pet.setPrimaryPhotoUrl("notfound");
        pet = petRepository.save(pet);

        // --- Auction ---
        LocalDateTime now = LocalDateTime.now();
        auction = new Auction();
        auction.setPet(pet);
        auction.setSellingUser(seller);
        auction.setDescription("Test auction");
        auction.setStartPrice(new BigDecimal("20.00"));
        auction.setHighestBid(new BigDecimal("20.00"));
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction = auctionRepository.save(auction);

        this.auctionId = auction.getAuctionId();
    }

    @Test
    @DisplayName("placeBid: creates winning bid, updates auction and calls wallet hold")
    @Transactional
    void placeBid_createsWinningBid_andUpdatesAuction() {
        // Arrange
        BigDecimal requiredHold = new BigDecimal("10.00");
        BigDecimal bidAmount = new BigDecimal("30.00");

        when(auctionPolicy.requireAmount(auctionId)).thenReturn(requiredHold);
        when(auctionPolicy.isValidIncrement(eq(auctionId), eq(bidAmount))).thenReturn(true);
        when(accountService.getAvailable(bidderId)).thenReturn(new BigDecimal("100.00"));
        // placeHold return value is ignored by BiddingService, so we can just stub it
        when(accountService.placeHold(eq(bidderId), eq(auctionId), eq(requiredHold))).thenReturn(null);

        // Act
        BidDto dto = biddingService.placeBid(bidderId, auctionId, bidAmount);

        // Assert: DTO basic sanity
        assertNotNull(dto);
        assertThat(dto.getAmount()).isEqualByComparingTo(bidAmount);

        // Assert: auction updated
        Auction reloadedAuction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(reloadedAuction.getHighestBid()).isEqualByComparingTo(bidAmount);
        assertNotNull(reloadedAuction.getWinningUser());
        assertEquals(bidderId, reloadedAuction.getWinningUser().getUserId());

        // Assert: winning bid persisted
        Bid winningBid = bidRepository.findTopByAuctionId(auctionId).orElseThrow();
        assertThat(winningBid.getAmount()).isEqualByComparingTo(bidAmount);
        assertEquals(Bid_Status.WINNING, winningBid.getBidStatus());
        assertEquals(bidderId, winningBid.getUser().getUserId());

        // Assert: wallet interactions
        verify(accountService).getAvailable(bidderId);
        verify(accountService).placeHold(bidderId, auctionId, requiredHold);
    }

    @Test
    @DisplayName("placeBid: non-LIVE auction is rejected")
    @Transactional
    void placeBid_nonLiveAuction_throws() {
        // Arrange: flip status to ENDED
        auction.setStatus(Auction_Status.ENDED);
        auctionRepository.save(auction);

        // Act + Assert
        var ex = assertThrows(
                AuctionInvalidStateException.class,
                () -> biddingService.placeBid(bidderId, auctionId, new BigDecimal("25.00"))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("live"));
        // Wallet should never be touched
        verifyNoInteractions(accountService);
    }

    @Test
    @DisplayName("placeBid: already-ended auction time is rejected")
    @Transactional
    void placeBid_endedByTime_throws() {
        // Arrange: set end time in the past, keep status LIVE
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        auctionRepository.save(auction);

        // Act + Assert
        var ex = assertThrows(
                AuctionInvalidStateException.class,
                () -> biddingService.placeBid(bidderId, auctionId, new BigDecimal("25.00"))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("already ended"));
        verifyNoInteractions(accountService);
    }

    @Test
    @DisplayName("placeBid: invalid increment rejected before wallet calls")
    @Transactional
    void placeBid_invalidIncrement_throws() {
        BigDecimal bidAmount = new BigDecimal("21.00");

        when(auctionPolicy.requireAmount(auctionId)).thenReturn(new BigDecimal("5.00"));
        when(auctionPolicy.isValidIncrement(eq(auctionId), eq(bidAmount))).thenReturn(false);

        var ex = assertThrows(
                InvalidBidException.class,
                () -> biddingService.placeBid(bidderId, auctionId, bidAmount)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("bid must be at least"));

        // No wallet calls for invalid increment
        verify(accountService, never()).getAvailable(anyLong());
        verify(accountService, never()).placeHold(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("placeBid: insufficient funds for required hold is rejected")
    @Transactional
    void placeBid_insufficientFunds_throws() {
        BigDecimal requiredHold = new BigDecimal("50.00");
        BigDecimal bidAmount = new BigDecimal("30.00");

        when(auctionPolicy.requireAmount(auctionId)).thenReturn(requiredHold);
        when(auctionPolicy.isValidIncrement(eq(auctionId), eq(bidAmount))).thenReturn(true);
        when(accountService.getAvailable(bidderId)).thenReturn(new BigDecimal("10.00"));

        var ex = assertThrows(
                InvalidBidException.class,
                () -> biddingService.placeBid(bidderId, auctionId, bidAmount)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient funds"));

        // Hold should not be placed when funds are insufficient
        verify(accountService).getAvailable(bidderId);
        verify(accountService, never()).placeHold(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("getWinningBid: returns highest bid DTO")
    @Transactional
    void getWinningBid_returnsHighestBid() {
        // Arrange: create a few bids directly
        Bid b1 = new Bid();
        b1.setAuction(auction);
        b1.setUser(bidder);
        b1.setAmount(new BigDecimal("22.00"));
        b1.setBidStatus(Bid_Status.WINNING);
        b1.setBidTime(LocalDateTime.now().minusMinutes(2));
        bidRepository.save(b1);

        Bid b2 = new Bid();
        b2.setAuction(auction);
        b2.setUser(bidder);
        b2.setAmount(new BigDecimal("25.00"));
        b2.setBidStatus(Bid_Status.WINNING);
        b2.setBidTime(LocalDateTime.now().minusMinutes(1));
        bidRepository.save(b2);

        // Act
        Optional<BidDto> result = biddingService.getWinningBid(auctionId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("getSecondHighestBid: returns runner-up bid DTO when available")
    @Transactional
    void getSecondHighestBid_returnsRunnerUp() {
        // Arrange
        Bid low = new Bid();
        low.setAuction(auction);
        low.setUser(bidder);
        low.setAmount(new BigDecimal("22.00"));
        low.setBidStatus(Bid_Status.OUTBID);
        low.setBidTime(LocalDateTime.now().minusMinutes(3));
        bidRepository.save(low);

        Bid mid = new Bid();
        mid.setAuction(auction);
        mid.setUser(bidder);
        mid.setAmount(new BigDecimal("26.00"));
        mid.setBidStatus(Bid_Status.OUTBID);
        mid.setBidTime(LocalDateTime.now().minusMinutes(2));
        bidRepository.save(mid);

        Bid high = new Bid();
        high.setAuction(auction);
        high.setUser(bidder);
        high.setAmount(new BigDecimal("30.00"));
        high.setBidStatus(Bid_Status.WINNING);
        high.setBidTime(LocalDateTime.now().minusMinutes(1));
        bidRepository.save(high);

        // Act
        Optional<BidDto> result = biddingService.getSecondHighestBid(auctionId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualByComparingTo("26.00");
    }

    @Test
    @DisplayName("finalizeBidsOnClose: marks top WINNING bid as WON and keeps others OUTBID")
    @Transactional
    void finalizeBidsOnClose_marksWinnerAndOutbidsOthers() {
        // Arrange
        Bid loser = new Bid();
        loser.setAuction(auction);
        loser.setUser(bidder);
        loser.setAmount(new BigDecimal("25.00"));
        loser.setBidStatus(Bid_Status.OUTBID);
        loser.setBidTime(LocalDateTime.now().minusMinutes(2));
        loser = bidRepository.save(loser);

        Bid winner = new Bid();
        winner.setAuction(auction);
        winner.setUser(bidder);
        winner.setAmount(new BigDecimal("30.00"));
        winner.setBidStatus(Bid_Status.WINNING);
        winner.setBidTime(LocalDateTime.now().minusMinutes(1));
        winner = bidRepository.save(winner);

        // Act
        biddingService.finalizeBidsOnClose(auctionId);

        // Assert
        Bid reloadedWinner = bidRepository.findById(winner.getBidId()).orElseThrow();
        Bid reloadedLoser = bidRepository.findById(loser.getBidId()).orElseThrow();

        assertEquals(Bid_Status.WON, reloadedWinner.getBidStatus());
        assertEquals(Bid_Status.OUTBID, reloadedLoser.getBidStatus());
    }

    @Test
    @DisplayName("finalizeBidsOnClose: no bids -> no-op and no exception")
    @Transactional
    void finalizeBidsOnClose_noBids_noop() {
        // Sanity: ensure auction currently has no bids
        assertTrue(auctionRepository.findById(auctionId).orElseThrow().getBids().isEmpty());

        // Should not throw
        assertDoesNotThrow(() -> biddingService.finalizeBidsOnClose(auctionId));
    }
}