package com.voti.pawction.services.auction;
import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.AccountExceptions.AccountNotFoundException;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.BidNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.InvalidBidException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.BidMapper;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.services.user.UserService;
import com.voti.pawction.services.wallet.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BiddingService}.
 *
 * Follows JUnit 5 + Mockito and uses your custom exceptions from
 * AccountExceptions, AuctionExceptions, BidExceptions, and UserExceptions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BiddingService unit tests")
class BiddingServiceTest {

    @Mock
    private BidMapper bidMapper;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionService auctionService;

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private Clock clock;

    @InjectMocks
    private BiddingService biddingService;

    private final Instant fixedInstant = Instant.parse("2025-01-01T12:00:00Z");
    private final ZoneId zone = ZoneId.of("UTC");

    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // Fixed clock so LocalDateTime.now(clock) is deterministic
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(zone);

        bidder = new User();
        bidder.setUserId(1L);

        auction = new Auction();
        auction.setAuctionId(100L);
        auction.setStatus(Auction_Status.LIVE);
        auction.setEndTime(LocalDateTime.ofInstant(fixedInstant, zone).plusHours(1));
    }

    // ---------------------------------------------------------------------
    // placeBid
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("placeBid")
    class PlaceBidTests {

        @Test
        @DisplayName("places bid successfully on LIVE, not-ended auction with valid increment")
        void placeBid_valid_createsBidAndUpdatesAuction() {
            // Arrange
            Long bidderId = 1L;
            Long auctionId = 100L;
            BigDecimal amount = BigDecimal.valueOf(50);
            BigDecimal requiredHold = BigDecimal.valueOf(10);

            when(userService.getUserOrThrow(bidderId)).thenReturn(bidder);
            when(auctionService.getAuctionOrThrowForUpdate(auctionId)).thenReturn(auction);

            when(auctionService.requireAmount(auctionId)).thenReturn(requiredHold);
            when(auctionService.isValidIncrement(auctionId, amount)).thenReturn(true);

            // available >= requiredHold so current code will place a hold
            when(accountService.getAvailable(bidderId)).thenReturn(BigDecimal.valueOf(100));

            Bid savedBid = new Bid();
            savedBid.setBidId(200L);
            savedBid.setAmount(amount);
            savedBid.setBidStatus(Bid_Status.WINNING);

            when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

            BidDto dto = new BidDto();
            dto.setBidId(200L);
            when(bidMapper.toDto(any(Bid.class))).thenReturn(dto);

            // Act
            BidDto result = biddingService.placeBid(bidderId, auctionId, amount);

            // Assert
            assertNotNull(result);
            assertEquals(200L, result.getBidId());

            // Bid object
            ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
            verify(bidRepository).save(bidCaptor.capture());
            Bid created = bidCaptor.getValue();
            assertEquals(amount, created.getAmount());
            assertEquals(Bid_Status.WINNING, created.getBidStatus());
            assertEquals(auction, created.getAuction());
            assertEquals(bidder, created.getUser());

            // Wallet hold
            verify(accountService).placeHold(bidderId, auctionId, requiredHold);

            // Outbid bulk update
            verify(bidRepository).bulkMarkOutbid(
                    eq(auctionId),
                    eq(savedBid.getBidId()),
                    eq(Bid_Status.OUTBID)
            );

            // Auction updated
            assertEquals(amount, auction.getHighestBid());
            assertEquals(bidder, auction.getWinningUser());
            verify(auctionRepository).save(auction);
        }

        @Test
        @DisplayName("throws InvalidAmountException when amount is <= 0")
        void placeBid_nonPositiveAmount_throwsInvalidAmountException() {
            Long bidderId = 1L;
            Long auctionId = 100L;

            assertThrows(InvalidAmountException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, BigDecimal.ZERO));

            assertThrows(InvalidAmountException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, BigDecimal.valueOf(-5)));
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction is not LIVE")
        void placeBid_auctionNotLive_throwsAuctionInvalidStateException() {
            // Arrange
            Long bidderId = 1L;
            Long auctionId = 100L;
            BigDecimal amount = BigDecimal.TEN;

            auction.setStatus(Auction_Status.CANCELED);

            when(userService.getUserOrThrow(bidderId)).thenReturn(bidder);
            when(auctionService.getAuctionOrThrowForUpdate(auctionId)).thenReturn(auction);

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, amount));
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction already ended")
        void placeBid_auctionEnded_throwsAuctionInvalidStateException() {
            // Arrange
            Long bidderId = 1L;
            Long auctionId = 100L;
            BigDecimal amount = BigDecimal.TEN;

            auction.setEndTime(LocalDateTime.ofInstant(fixedInstant, zone).minusMinutes(1));

            when(userService.getUserOrThrow(bidderId)).thenReturn(bidder);
            when(auctionService.getAuctionOrThrowForUpdate(auctionId)).thenReturn(auction);

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, amount));
        }

        @Test
        @DisplayName("throws InvalidBidException when increment is invalid")
        void placeBid_invalidIncrement_throwsInvalidBidException() {
            // Arrange
            Long bidderId = 1L;
            Long auctionId = 100L;
            BigDecimal amount = BigDecimal.TEN;

            when(userService.getUserOrThrow(bidderId)).thenReturn(bidder);
            when(auctionService.getAuctionOrThrowForUpdate(auctionId)).thenReturn(auction);
            when(auctionService.requireAmount(auctionId)).thenReturn(BigDecimal.ONE);
            when(auctionService.isValidIncrement(auctionId, amount)).thenReturn(false);

            // Act + Assert
            assertThrows(InvalidBidException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, amount));

            verify(accountService, never()).placeHold(anyLong(), anyLong(), any());
            verify(bidRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates UserNotFoundException when bidder does not exist")
        void placeBid_missingBidder_throwsUserNotFoundException() {
            // Arrange
            Long bidderId = 99L;
            Long auctionId = 100L;
            BigDecimal amount = BigDecimal.TEN;

            when(userService.getUserOrThrow(bidderId))
                    .thenThrow(new UserNotFoundException("user not found"));

            // Act + Assert
            assertThrows(UserNotFoundException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, amount));
        }

        @Test
        @DisplayName("propagates AuctionNotFoundException when auction does not exist")
        void placeBid_missingAuction_throwsAuctionNotFoundException() {
            // Arrange
            Long bidderId = 1L;
            Long auctionId = 999L;
            BigDecimal amount = BigDecimal.TEN;

            when(userService.getUserOrThrow(bidderId)).thenReturn(bidder);
            when(auctionService.getAuctionOrThrowForUpdate(auctionId))
                    .thenThrow(new AuctionNotFoundException("auction not found"));

            // Act + Assert
            assertThrows(AuctionNotFoundException.class,
                    () -> biddingService.placeBid(bidderId, auctionId, amount));
        }
    }

    // ---------------------------------------------------------------------
    // getWinningBid
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("getWinningBid")
    class GetWinningBidTests {

        @Test
        @DisplayName("returns Optional with mapped DTO when top bid exists")
        void getWinningBid_existingBid_returnsOptionalDto() {
            // Arrange
            Long auctionId = 100L;
            Bid topBid = new Bid();
            topBid.setBidId(200L);

            BidDto dto = new BidDto();
            dto.setBidId(200L);

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findTopByAuctionId(auction.getAuctionId()))
                    .thenReturn(Optional.of(topBid));
            when(bidMapper.toDto(topBid)).thenReturn(dto);

            // Act
            Optional<BidDto> result = biddingService.getWinningBid(auctionId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(200L, result.get().getBidId());
        }

        @Test
        @DisplayName("throws BidNotFoundException when there are no bids")
        void getWinningBid_noBids_throwsBidNotFoundException() {
            // Arrange
            Long auctionId = 100L;

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findTopByAuctionId(auction.getAuctionId()))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(BidNotFoundException.class,
                    () -> biddingService.getWinningBid(auctionId));
        }
    }

    // ---------------------------------------------------------------------
    // getSecondHighestBid
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("getSecondHighestBid")
    class GetSecondHighestBidTests {

        @Test
        @DisplayName("returns Optional with mapped DTO when second bid exists")
        void getSecondHighestBid_existingBid_returnsOptionalDto() {
            // Arrange
            Long auctionId = 100L;
            Bid second = new Bid();
            second.setBidId(300L);

            BidDto dto = new BidDto();
            dto.setBidId(300L);

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findSecondByAuctionId(auction.getAuctionId()))
                    .thenReturn(Optional.of(second));
            when(bidMapper.toDto(second)).thenReturn(dto);

            // Act
            Optional<BidDto> result = biddingService.getSecondHighestBid(auctionId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(300L, result.get().getBidId());
        }

        @Test
        @DisplayName("throws BidNotFoundException when there is no second bid")
        void getSecondHighestBid_noSecondBid_throwsBidNotFoundException() {
            // Arrange
            Long auctionId = 100L;

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findSecondByAuctionId(auction.getAuctionId()))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(BidNotFoundException.class,
                    () -> biddingService.getSecondHighestBid(auctionId));
        }
    }

    // ---------------------------------------------------------------------
    // finalizeBidsOnClose
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("finalizeBidsOnClose")
    class FinalizeBidsOnCloseTests {

        @Test
        @DisplayName("does nothing when there are no bids")
        void finalizeBidsOnClose_noBids_doesNothing() {
            // Arrange
            Long auctionId = 100L;

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findByAuctionIdOrderByAmountDescBidTimeAsc(auction.getAuctionId()))
                    .thenReturn(List.of());

            // Act
            biddingService.finalizeBidsOnClose(auctionId);

            // Assert
            verify(bidRepository, never()).findTopByAuctionId(anyLong());
            verify(bidRepository, never()).bulkMarkOutbid(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("marks winning bid as WON and others as OUTBID")
        void finalizeBidsOnClose_withWinningBid_marksStatuses() {
            // Arrange
            Long auctionId = 100L;

            Bid winning = new Bid();
            winning.setBidId(200L);
            winning.setBidStatus(Bid_Status.WINNING);

            Bid other = new Bid();
            other.setBidId(201L);
            other.setBidStatus(Bid_Status.WINNING);

            when(auctionService.getAuctionOrThrow(auctionId)).thenReturn(auction);
            when(bidRepository.findByAuctionIdOrderByAmountDescBidTimeAsc(auction.getAuctionId()))
                    .thenReturn(List.of(winning, other));
            when(bidRepository.findTopByAuctionId(auction.getAuctionId()))
                    .thenReturn(Optional.of(winning));

            // Act
            biddingService.finalizeBidsOnClose(auctionId);

            // Assert
            assertEquals(Bid_Status.WON, winning.getBidStatus());
            verify(bidRepository).save(winning);
            verify(bidRepository).bulkMarkOutbid(
                    eq(auctionId),
                    eq(winning.getBidId()),
                    eq(Bid_Status.OUTBID)
            );
        }
    }

    // ---------------------------------------------------------------------
    // Lookup helpers
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("lookup helpers")
    class LookupTests {

        @Test
        @DisplayName("getBidOrThrow returns bid when present")
        void getBidOrThrow_present_returnsBid() {
            Long bidId = 10L;
            Bid bid = new Bid();
            bid.setBidId(bidId);

            when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

            Bid result = biddingService.getBidOrThrow(bidId);
            assertEquals(bidId, result.getBidId());
        }

        @Test
        @DisplayName("getBidOrThrow throws BidNotFoundException when missing")
        void getBidOrThrow_missing_throwsBidNotFoundException() {
            Long bidId = 10L;
            when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

            assertThrows(BidNotFoundException.class,
                    () -> biddingService.getBidOrThrow(bidId));
        }


    }
}