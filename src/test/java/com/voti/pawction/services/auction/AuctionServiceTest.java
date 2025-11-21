package com.voti.pawction.services.auction;
import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionEndTimeRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdatePetWhenAuctionLiveRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.AuctionExceptions.InvalidAuctionException;
import com.voti.pawction.exceptions.PetNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.AuctionMapper;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.auction.impl.AuctionServiceInterface;
import com.voti.pawction.services.auction.policy.AuctionPolicy;
import com.voti.pawction.services.pet.PetService;
import com.voti.pawction.services.user.UserService;
import com.voti.pawction.services.auction.BiddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuctionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService unit tests")
class AuctionServiceTest {

    @Mock
    private AuctionMapper auctionMapper;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserService userService;

    @Mock
    private PetService petService;

    @Mock
    private BiddingService biddingService;

    @InjectMocks
    private AuctionService auctionService;

    // we will override the clock via reflection if needed, but simplest is:
    private Clock fixedClock;

    @BeforeEach
    void setUp() throws Exception {
        fixedClock = Clock.fixed(
                Instant.parse("2025-01-01T12:00:00Z"),
                ZoneId.of("UTC")
        );

        // manually inject clock since @InjectMocks can't set it to fixedClock automatically
        var clockField = AuctionService.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(auctionService, fixedClock);
    }

    // ---------------------------------------------------------------------
    // create
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates a LIVE auction with valid input")
        void create_validRequest_createsAuction() {
            // Arrange
            Long sellerId = 1L;
            Long petId = 10L;

            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setStartPrice(BigDecimal.valueOf(50));
            request.setDescription("Nice dog");
            request.setEndedAt(LocalDateTime.now(fixedClock).plusHours(12));

            User seller = new User();
            seller.setUserId(sellerId);

            Pet pet = new Pet();
            pet.setPetId(petId);

            Auction saved = new Auction();
            saved.setAuctionId(100L);

            when(userService.getUserOrThrow(sellerId)).thenReturn(seller);
            when(petService.getPetOrThrow(petId)).thenReturn(pet);
            when(auctionRepository.save(any(Auction.class))).thenReturn(saved);
            when(auctionMapper.toDto(saved)).thenReturn(new AuctionDto());

            // Act
            AuctionDto result = auctionService.create(sellerId, petId, request);

            // Assert
            assertNotNull(result);
            verify(userService).getUserOrThrow(sellerId);
            verify(petService).getPetOrThrow(petId);
            verify(auctionRepository).save(any(Auction.class));
            verify(auctionMapper).toDto(saved);
        }

        @Test
        @DisplayName("throws InvalidAuctionException when description is blank")
        void create_blankDescription_throwsInvalidAuctionException() {
            // Arrange
            Long sellerId = 1L;
            Long petId = 10L;

            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setStartPrice(BigDecimal.valueOf(10));
            request.setDescription("   "); // blank
            request.setEndedAt(LocalDateTime.now(fixedClock).plusHours(12));

            // seller + pet are fine
            when(userService.getUserOrThrow(sellerId)).thenReturn(new User());
            when(petService.getPetOrThrow(petId)).thenReturn(new Pet());

            // Act + Assert
            assertThrows(InvalidAuctionException.class,
                    () -> auctionService.create(sellerId, petId, request));
        }

        @Test
        @DisplayName("throws InvalidAuctionException when end time is not 12 hours from now")
        void create_invalidEndTime_throwsInvalidAuctionException() {
            // Arrange
            Long sellerId = 1L;
            Long petId = 10L;

            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setStartPrice(BigDecimal.valueOf(10));
            request.setDescription("desc");
            // 10 hours instead of 12
            request.setEndedAt(LocalDateTime.now(fixedClock).plusHours(10));

            when(userService.getUserOrThrow(sellerId)).thenReturn(new User());
            when(petService.getPetOrThrow(petId)).thenReturn(new Pet());

            // Act + Assert
            assertThrows(InvalidAuctionException.class,
                    () -> auctionService.create(sellerId, petId, request));
        }

        @Test
        @DisplayName("throws InvalidAmountException when start price is <= 0")
        void create_invalidStartPrice_throwsInvalidAmountException() {
            // Arrange
            Long sellerId = 1L;
            Long petId = 10L;

            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setStartPrice(BigDecimal.ZERO);
            request.setDescription("desc");
            request.setEndedAt(LocalDateTime.now(fixedClock).plusHours(12));

            when(userService.getUserOrThrow(sellerId)).thenReturn(new User());
            when(petService.getPetOrThrow(petId)).thenReturn(new Pet());

            // Act + Assert
            assertThrows(InvalidAuctionException.class, // or InvalidAmountException if you throw that
                    () -> auctionService.create(sellerId, petId, request));
        }
    }

    // ---------------------------------------------------------------------
    // updateAuctionDetail
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("updateAuctionDetail")
    class UpdateAuctionDetailTests {

        @Test
        @DisplayName("updates description for LIVE auction")
        void updateAuctionDetail_live_updatesDescription() {
            // Arrange
            Long auctionId = 100L;
            UpdateAuctionDetailRequest request = new UpdateAuctionDetailRequest();
            request.setDescription("new description");

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);

            when(auctionRepository.findByIdForUpdate(auctionId))
                    .thenReturn(Optional.of(auction));
            when(auctionRepository.save(auction))
                    .thenReturn(auction);
            when(auctionMapper.toDto(auction))
                    .thenReturn(new AuctionDto());

            // Act
            AuctionDto result = auctionService.updateAuctionDetail(auctionId, request);

            // Assert
            assertNotNull(result);
            assertEquals("new description", auction.getDescription());
            verify(auctionRepository).findByIdForUpdate(auctionId);
            verify(auctionRepository).save(auction);
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction is not LIVE")
        void updateAuctionDetail_notLive_throwsAuctionInvalidStateException() {
            // Arrange
            Long auctionId = 100L;
            UpdateAuctionDetailRequest request = new UpdateAuctionDetailRequest();
            request.setDescription("desc");

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.ENDED);

            when(auctionRepository.findByIdForUpdate(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> auctionService.updateAuctionDetail(auctionId, request));
        }

        @Test
        @DisplayName("throws InvalidAuctionException when description is blank")
        void updateAuctionDetail_blankDescription_throwsInvalidAuctionException() {
            // Arrange
            Long auctionId = 100L;
            UpdateAuctionDetailRequest request = new UpdateAuctionDetailRequest();
            request.setDescription("   ");

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);

            when(auctionRepository.findByIdForUpdate(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(InvalidAuctionException.class,
                    () -> auctionService.updateAuctionDetail(auctionId, request));
        }
    }

    // ---------------------------------------------------------------------
    // updateAuctionEndTime
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("updateAuctionEndTime")
    class UpdateAuctionEndTimeTests {

        @Test
        @DisplayName("updates end time for LIVE auction when new time is valid")
        void updateAuctionEndTime_valid_updatesEndTime() {
            // Arrange
            Long auctionId = 100L;

            UpdateAuctionEndTimeRequest request = new UpdateAuctionEndTimeRequest();
            LocalDateTime newEnd = LocalDateTime.now(fixedClock).plusHours(12);
            request.setNewEndTime(newEnd);

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);
            auction.setCreatedAt(LocalDateTime.now(fixedClock));

            when(auctionRepository.findByIdForUpdate(auctionId))
                    .thenReturn(Optional.of(auction));
            when(auctionRepository.save(auction))
                    .thenReturn(auction);
            when(auctionMapper.toDto(auction))
                    .thenReturn(new AuctionDto());

            // Act
            AuctionDto result = auctionService.updateAuctionEndTime(auctionId, request);

            // Assert
            assertNotNull(result);
            assertEquals(newEnd, auction.getEndTime());
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction is not LIVE")
        void updateAuctionEndTime_notLive_throwsAuctionInvalidStateException() {
            // Arrange
            Long auctionId = 100L;
            UpdateAuctionEndTimeRequest request = new UpdateAuctionEndTimeRequest();
            request.setNewEndTime(LocalDateTime.now(fixedClock).plusHours(12));

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.CANCELED);
            auction.setCreatedAt(LocalDateTime.now(fixedClock));

            when(auctionRepository.findByIdForUpdate(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> auctionService.updateAuctionEndTime(auctionId, request));
        }
    }

    // ---------------------------------------------------------------------
    // cancel
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("cancels LIVE auction before first bid")
        void cancel_liveNoBid_setsCanceled() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);
            auction.setStartPrice(BigDecimal.TEN);
            auction.setHighestBid(BigDecimal.TEN); // no bid yet

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act
            auctionService.cancel(auctionId);

            // Assert
            assertEquals(Auction_Status.CANCELED, auction.getStatus());
            verify(auctionRepository).save(auction);
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction is not LIVE")
        void cancel_notLive_throwsAuctionInvalidStateException() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.ENDED);

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> auctionService.cancel(auctionId));
        }

        @Test
        @DisplayName("throws InvalidAuctionException when first bid already occurred")
        void cancel_afterFirstBid_throwsInvalidAuctionException() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);
            auction.setStartPrice(BigDecimal.TEN);
            auction.setHighestBid(BigDecimal.valueOf(20)); // has bid

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(InvalidAuctionException.class,
                    () -> auctionService.cancel(auctionId));
        }
    }

    // ---------------------------------------------------------------------
    // end
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("end")
    class EndTests {

        @Test
        @DisplayName("ends auction with no bids (no winner)")
        void end_noBids_leavesNoWinner() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));
            when(auctionMapper.toDto(auction))
                    .thenReturn(new AuctionDto());
            when(biddingService.getWinningBid(auctionId))
                    .thenReturn(Optional.empty());

            // Act
            AuctionDto result = auctionService.end(auctionId);

            // Assert
            assertNotNull(result);
            assertEquals(Auction_Status.ENDED, auction.getStatus());
            verify(biddingService, never()).finalizeBidsOnClose(anyLong());
        }

        @Test
        @DisplayName("throws AuctionInvalidStateException when auction is already ENDED or CANCELED")
        void end_invalidState_throwsAuctionInvalidStateException() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.CANCELED);

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));

            // Act + Assert
            assertThrows(AuctionInvalidStateException.class,
                    () -> auctionService.end(auctionId));
        }

        @Test
        @DisplayName("ends auction with winner and finalizes bids")
        void end_withWinner_finalizesBids() {
            // Arrange
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);

            // You will need to create a bid in the database
            BidDto winningBid = new BidDto();
            winningBid.setBidId(200L);

            when(auctionRepository.findById(auctionId))
                    .thenReturn(Optional.of(auction));
            when(biddingService.getWinningBid(auctionId))
                    .thenReturn(Optional.of(winningBid));
            when(auctionMapper.toDto(auction))
                    .thenReturn(new AuctionDto());

            User winner = new User();
            winner.setUserId(999L);
            when(userService.getUserOrThrow(200L))
                    .thenReturn(winner);

            // Act
            AuctionDto result = auctionService.end(auctionId);

            // Assert
            assertNotNull(result);
            assertEquals(Auction_Status.ENDED, auction.getStatus());
            verify(biddingService).finalizeBidsOnClose(auctionId);
            verify(userService).getUserOrThrow(200L);
        }
    }
}