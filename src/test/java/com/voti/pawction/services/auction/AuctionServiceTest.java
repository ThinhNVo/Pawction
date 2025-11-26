package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionEndTimeRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdatePetWhenAuctionLiveRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.AuctionExceptions.InvalidAuctionException;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuctionServiceTest {

    @Autowired private AuctionService auctionService;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private AuctionRepository auctionRepository;

    private Long sellerId;
    private Long petId;
    private Long auctionId;

    @BeforeEach
    @Transactional
    void setUp() {
        // --- Seller ---
        User seller = new User();
        seller.setName("Seller User");
        seller.setEmail("seller@example.com");
        seller.setPasswordHash("secret");
        seller = userRepository.save(seller);
        this.sellerId = seller.getUserId();

        // --- Pet ---
        Pet pet = new Pet();
        pet.setPetName("Auction Pet");
        pet.setPetAgeMonths(18);
        pet.setPetSex(Sex.M);
        pet.setPetWeight(8.5);
        pet.setPetCategory(Category.Dog);
        pet.setDogBreed("Beagle");
        pet.setDogSize(Size.MEDIUM);
        pet.setDogTemperament("Friendly");
        pet.setDogIsHypoallergenic(Allergy.UNKNOWN);
        pet.setPrimaryPhotoUrl("photo-url");
        pet.setOwner(seller);
        pet = petRepository.save(pet);
        this.petId = pet.getPetId();

        // --- Initial LIVE auction used by many tests ---
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setStartPrice(new BigDecimal("20.00"));
        req.setDescription("Initial auction");
        req.setEndedAt(LocalDateTime.now().plusDays(1));

        AuctionDto created = auctionService.create(sellerId, petId, req);
        this.auctionId = created.getAuctionId();

        assertNotNull(sellerId);
        assertNotNull(petId);
        assertNotNull(auctionId);

        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(Auction_Status.LIVE);
        assertThat(a.getHighestBid()).isEqualByComparingTo("20.00");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("create: creates LIVE auction with highestBid = startPrice")
    void create_createsLiveAuction() {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setStartPrice(new BigDecimal("50.00"));
        req.setDescription("New auction");
        req.setEndedAt(LocalDateTime.now().plusDays(2));

        AuctionDto dto = auctionService.create(sellerId, petId, req);

        assertNotNull(dto.getAuctionId());
        Auction persisted = auctionRepository.findById(dto.getAuctionId()).orElseThrow();

        assertThat(persisted.getStartPrice()).isEqualByComparingTo("50.00");
        assertThat(persisted.getHighestBid()).isEqualByComparingTo("50.00");
        assertThat(persisted.getStatus()).isEqualTo(Auction_Status.LIVE);
        assertThat(persisted.getPet().getPetId()).isEqualTo(petId);
        assertThat(persisted.getSellingUser().getUserId()).isEqualTo(sellerId);
    }

    @Test
    @Transactional
    @DisplayName("create: non-positive startPrice throws InvalidAmountException")
    void create_nonPositiveStartPrice_throws() {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setStartPrice(BigDecimal.ZERO);
        req.setDescription("Invalid auction");
        req.setEndedAt(LocalDateTime.now().plusDays(1));

        assertThrows(InvalidAmountException.class,
                () -> auctionService.create(sellerId, petId, req));
    }

    @Test
    @Transactional
    @DisplayName("create: blank description throws InvalidAuctionException")
    void create_blankDescription_throws() {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setStartPrice(new BigDecimal("10.00"));
        req.setDescription("   ");
        req.setEndedAt(LocalDateTime.now().plusDays(1));

        assertThrows(InvalidAuctionException.class,
                () -> auctionService.create(sellerId, petId, req));
    }

    // -------------------------------------------------------------------------
    // updateAuctionDetail
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateAuctionDetail: updates description for LIVE auction")
    void updateAuctionDetail_updatesDescription() {
        UpdateAuctionDetailRequest req = new UpdateAuctionDetailRequest();
        req.setDescription("Updated description");

        AuctionDto dto = auctionService.updateAuctionDetail(auctionId, req);

        Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("Updated description");
        assertThat(dto.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @Transactional
    @DisplayName("updateAuctionDetail: non-LIVE auction throws AuctionInvalidStateException")
    void updateAuctionDetail_nonLive_throws() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        a.setStatus(Auction_Status.ENDED);
        auctionRepository.save(a);

        UpdateAuctionDetailRequest req = new UpdateAuctionDetailRequest();
        req.setDescription("Should fail");

        assertThrows(AuctionInvalidStateException.class,
                () -> auctionService.updateAuctionDetail(auctionId, req));
    }

    // -------------------------------------------------------------------------
    // updateAuctionEndTime (negative path only; positive path depends on time window rules)
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateAuctionEndTime: non-LIVE auction throws AuctionInvalidStateException")
    void updateAuctionEndTime_nonLive_throws() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        a.setStatus(Auction_Status.CANCELED);
        auctionRepository.save(a);

        UpdateAuctionEndTimeRequest req = new UpdateAuctionEndTimeRequest();
        req.setNewEndTime(LocalDateTime.now().plusDays(1));

        assertThrows(AuctionInvalidStateException.class,
                () -> auctionService.updateAuctionEndTime(auctionId, req));
    }

    // -------------------------------------------------------------------------
    // updateAuctionPetInfo
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateAuctionPetInfo: seller can update limited pet info while LIVE")
    void updateAuctionPetInfo_updatesPetWhenSellerMatches() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        Long currentPetId = a.getPet().getPetId();

        UpdatePetWhenAuctionLiveRequest req = new UpdatePetWhenAuctionLiveRequest();
        req.setPetName("Updated Auction Pet");
        req.setPetAgeMonths(20);
        req.setPetWeight(9.0);

        AuctionDto dto = auctionService.updateAuctionPetInfo(auctionId, sellerId, req);

        Auction reloadedAuction = auctionRepository.findById(auctionId).orElseThrow();
        Pet reloadedPet = petRepository.findById(currentPetId).orElseThrow();

        assertThat(dto.getAuctionId()).isEqualTo(auctionId);
        assertThat(reloadedPet.getPetName()).isEqualTo("Updated Auction Pet");
        assertThat(reloadedPet.getPetAgeMonths()).isEqualTo(20);
        assertThat(reloadedPet.getPetWeight()).isEqualTo(9.0);
    }

    @Test
    @Transactional
    @DisplayName("updateAuctionPetInfo: non-seller cannot update pet info")
    void updateAuctionPetInfo_nonSeller_throws() {
        // create another user
        User other = new User();
        other.setName("Other Seller");
        other.setEmail("other@example.com");
        other.setPasswordHash("secret");
        other = userRepository.save(other);

        UpdatePetWhenAuctionLiveRequest req = new UpdatePetWhenAuctionLiveRequest();
        req.setPetName("Should not apply");
        req.setPetAgeMonths(12);
        req.setPetWeight(5.0);

        Long otherId = other.getUserId();

        assertThrows(IllegalArgumentException.class,
                () -> auctionService.updateAuctionPetInfo(auctionId, otherId, req));
    }

    @Test
    @Transactional
    @DisplayName("updateAuctionPetInfo: non-LIVE auction throws AuctionInvalidStateException")
    void updateAuctionPetInfo_nonLive_throws() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        a.setStatus(Auction_Status.ENDED);
        auctionRepository.save(a);

        UpdatePetWhenAuctionLiveRequest req = new UpdatePetWhenAuctionLiveRequest();
        req.setPetName("Should fail");
        req.setPetAgeMonths(10);
        req.setPetWeight(6.0);

        assertThrows(AuctionInvalidStateException.class,
                () -> auctionService.updateAuctionPetInfo(auctionId, sellerId, req));
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @Transactional
        @DisplayName("cancel: LIVE auction with no bids transitions to CANCELED")
        void cancel_liveNoBids_setsCanceled() {
            auctionService.cancel(auctionId);

            Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(Auction_Status.CANCELED);
        }

        @Test
        @Transactional
        @DisplayName("cancel: non-LIVE auction throws AuctionInvalidStateException")
        void cancel_nonLive_throws() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setStatus(Auction_Status.ENDED);
            auctionRepository.save(a);

            assertThrows(AuctionInvalidStateException.class,
                    () -> auctionService.cancel(auctionId));
        }

        @Test
        @Transactional
        @DisplayName("cancel: after first bid (highestBid > startPrice) throws InvalidAuctionException")
        void cancel_afterFirstBid_throwsInvalidAuction() {
            Auction a = auctionRepository.findById(auctionId).orElseThrow();
            a.setHighestBid(a.getStartPrice().add(BigDecimal.ONE));
            auctionRepository.save(a);

            assertThrows(InvalidAuctionException.class,
                    () -> auctionService.cancel(auctionId));
        }
    }

    // -------------------------------------------------------------------------
    // end / endEarly
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("end: LIVE auction with no bids -> SETTLED and no winner (via settlementService.noWinner)")
    void end_liveNoBids_setsSettledNoWinner() {
        AuctionDto dto = auctionService.end(auctionId);

        Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(dto.getStatus()).isEqualTo(Auction_Status.SETTLED);
        assertThat(reloaded.getStatus()).isEqualTo(Auction_Status.SETTLED);
        assertThat(reloaded.getWinningUser()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("end: CANCELED auction throws AuctionInvalidStateException")
    void end_canceled_throws() {
        Auction a = auctionRepository.findById(auctionId).orElseThrow();
        a.setStatus(Auction_Status.CANCELED);
        auctionRepository.save(a);

        assertThrows(AuctionInvalidStateException.class,
                () -> auctionService.end(auctionId));
    }

    @Test
    @Transactional
    @DisplayName("endEarly: LIVE auction is ended and settled when no bids")
    void endEarly_noBids_setsSettled() {
        AuctionDto dto = auctionService.endEarly(auctionId);

        Auction reloaded = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Auction_Status.SETTLED);
        assertThat(dto.getStatus()).isEqualTo(Auction_Status.SETTLED);
        assertThat(reloaded.getWinningUser()).isNull();
        assertThat(reloaded.getEndTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }


}