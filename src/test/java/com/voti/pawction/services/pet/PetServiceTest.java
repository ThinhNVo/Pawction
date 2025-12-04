package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.PetRequest.RegisterCatRequest;
import com.voti.pawction.dtos.request.PetRequest.RegisterDogRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdateCatRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdateDogRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdatePetWhenAuctionLiveRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Payment_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.exceptions.PetExceptions.InvalidStateException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PetServiceTest {

    @Autowired private PetService petService;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private AuctionRepository auctionRepository;

    private Long ownerId;
    private Long existingDogId;

    @BeforeEach
    @Transactional
    void setUpOwnerAndDog() {
        // --- Owner ---
        User owner = new User();
        owner.setName("Pet Owner");
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("secret");
        owner = userRepository.save(owner);
        this.ownerId = owner.getUserId();

        // --- Existing dog (for update tests) ---
        Pet dog = new Pet();
        dog.setPetName("Rex");
        dog.setPetAgeMonths(24);
        dog.setPetSex(Sex.M);
        dog.setPetWeight(15.0);
        dog.setPetCategory(Category.Dog);
        dog.setDogBreed("Beagle");
        dog.setDogSize(Size.MEDIUM);
        dog.setDogTemperament("Friendly");
        dog.setDogIsHypoallergenic(Allergy.UNKNOWN);
        dog.setPrimaryPhotoUrl("initial-photo");
        dog.setOwner(owner);

        dog = petRepository.save(dog);
        this.existingDogId = dog.getPetId();

        assertNotNull(ownerId);
        assertNotNull(existingDogId);
    }

    // -------------------------------------------------------------------------
    // registerDog
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("registerDog: creates a dog under the given owner")
    void registerDog_createsDogUnderOwner() {
        long beforeCount = petRepository.count();

        RegisterDogRequest req = new RegisterDogRequest();
        req.setPetName("Buddy");
        req.setPetAgeMonths(12);
        req.setPetSex(Sex.M);
        req.setPetWeight(10.5);
        req.setPetCategory(Category.Dog);
        req.setDogBreed("Corgi");
        req.setDogSize(Size.SMALL);
        req.setDogTemperament("Playful");
        req.setDogIsHypoallergenic(Allergy.NO);
        req.setPrimaryPhoto(new MockMultipartFile(
                "photo", "buddy.jpg", "image/jpeg", new byte[]{1, 2, 3}
        ));

        PetDto created = petService.registerDog(ownerId, req);

        assertNotNull(created);
        assertNotNull(created.getPetId());
        assertThat(petRepository.count()).isEqualTo(beforeCount + 1);

        Pet reloaded = petRepository.findById(created.getPetId()).orElseThrow();
        assertThat(reloaded.getPetName()).isEqualTo("Buddy");
        assertThat(reloaded.getPetCategory()).isEqualTo(Category.Dog);
        assertThat(reloaded.getOwner().getUserId()).isEqualTo(ownerId);
    }

    @Test
    @Transactional
    @DisplayName("registerDog: missing name throws ValidationException")
    void registerDog_missingName_throwsValidation() {
        RegisterDogRequest req = new RegisterDogRequest();
        req.setPetName(null); // invalid
        req.setPetAgeMonths(12);
        req.setPetSex(Sex.F);
        req.setPetWeight(8.0);
        req.setPetCategory(Category.Dog);
        req.setDogBreed("Collie");
        req.setDogSize(Size.MEDIUM);
        req.setDogTemperament("Calm");
        req.setDogIsHypoallergenic(Allergy.YES);
        req.setPrimaryPhoto(new MockMultipartFile(
                "photo", "collie.jpg", "image/jpeg", new byte[]{1}
        ));

        assertThrows(ValidationException.class,
                () -> petService.registerDog(ownerId, req));
    }

    // -------------------------------------------------------------------------
    // registerCat
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("registerCat: creates a cat under the given owner")
    void registerCat_createsCatUnderOwner() {
        RegisterCatRequest req = new RegisterCatRequest();
        req.setPetName("Whiskers");
        req.setPetAgeMonths(18);
        req.setPetSex(Sex.F);
        req.setPetWeight(4.2);
        req.setPetCategory(Category.Cat);
        req.setCatBreed("Siamese");
        req.setCatCoatLength(Coat_Length.SHORT);
        req.setCatIndoorOnly(Indoor.YES);
        req.setPrimaryPhoto(new MockMultipartFile(
                "photo", "cat.jpg", "image/jpeg", new byte[]{4, 5}
        ));

        PetDto created = petService.registerCat(ownerId, req);

        assertNotNull(created);
        Pet persisted = petRepository.findById(created.getPetId()).orElseThrow();
        assertThat(persisted.getPetCategory()).isEqualTo(Category.Cat);
        assertThat(persisted.getOwner().getUserId()).isEqualTo(ownerId);
    }

    // -------------------------------------------------------------------------
    // updateDog
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateDog: valid request updates dog fields")
    void updateDog_updatesDogFields() {
        UpdateDogRequest req = new UpdateDogRequest();
        req.setPetName("Rex Updated");
        req.setPetAgeMonths(30);
        req.setPetSex(Sex.M);
        req.setPetWeight(16.0);
        req.setDogBreed("Beagle Mix");
        req.setDogSize(Size.MEDIUM);
        req.setDogTemperament("Very friendly");
        req.setDogIsHypoallergenic(Allergy.UNKNOWN);

        PetDto updated = petService.updateDog(existingDogId, ownerId, req);

        assertNotNull(updated);
        Pet reloaded = petRepository.findById(existingDogId).orElseThrow();
        assertThat(reloaded.getPetName()).isEqualTo("Rex Updated");
        assertThat(reloaded.getPetWeight()).isEqualTo(16.0);
        assertThat(reloaded.getDogBreed()).isEqualTo("Beagle Mix");
    }

    @Test
    @Transactional
    @DisplayName("updateDog: non-owner cannot update pet")
    void updateDog_nonOwner_throwsIllegalArgument() {
        // another user
        User other = new User();
        other.setName("Other Owner");
        other.setEmail("other@example.com");
        other.setPasswordHash("secret");
        other = userRepository.save(other);
        Long otherId = other.getUserId();

        UpdateDogRequest req = new UpdateDogRequest();
        req.setPetName("Should Fail");
        req.setPetAgeMonths(24);
        req.setPetSex(Sex.F);
        req.setPetWeight(9.5);
        req.setDogBreed("Pug");
        req.setDogSize(Size.TOY);
        req.setDogTemperament("Cute");
        req.setDogIsHypoallergenic(Allergy.NO);

        assertThrows(IllegalArgumentException.class,
                () -> petService.updateDog(existingDogId, otherId, req));
    }

    @Test
    @Transactional
    @DisplayName("updateDog: pet locked in auction throws InvalidStateException")
    void updateDog_petLockedInAuction_throwsInvalidState() {
        Pet lockedPet = petRepository.findById(existingDogId).orElseThrow();

        // attach auction for this pet so existsByPet_PetId returns true
        User seller = lockedPet.getOwner();
        LocalDateTime now = LocalDateTime.now();

        Auction auction = new Auction();
        auction.setPet(lockedPet);
        auction.setSellingUser(seller);
        auction.setStartPrice(java.math.BigDecimal.ONE);
        auction.setHighestBid(java.math.BigDecimal.ONE);
        auction.setDescription("Locked auction");
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction.setPaymentDueDate(null);
        auction.setPaymentStatus(Payment_Status.UNPAID); // adjust if non-nullable in entity
        auctionRepository.save(auction);

        UpdateDogRequest req = new UpdateDogRequest();
        req.setPetName("Locked");
        req.setPetAgeMonths(24);
        req.setPetSex(Sex.M);
        req.setPetWeight(14.0);
        req.setDogBreed("Beagle");
        req.setDogSize(Size.MEDIUM);
        req.setDogTemperament("Friendly");
        req.setDogIsHypoallergenic(Allergy.UNKNOWN);

        assertThrows(InvalidStateException.class,
                () -> petService.updateDog(existingDogId, ownerId, req));
    }

    // -------------------------------------------------------------------------
    // updateCat + auction lock
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateCat: pet in auction throws InvalidStateException")
    void updateCat_petLockedInAuction_throwsInvalidState() {
        // Create cat
        Pet cat = new Pet();
        cat.setPetName("Mimi");
        cat.setPetAgeMonths(10);
        cat.setPetSex(Sex.F);
        cat.setPetWeight(3.5);
        cat.setPetCategory(Category.Cat);
        cat.setCatBreed("Tabby");
        cat.setCatCoatLength(Coat_Length.MEDIUM);
        cat.setCatIndoorOnly(Indoor.YES);
        cat.setOwner(userRepository.findById(ownerId).orElseThrow());
        cat.setPrimaryPhotoUrl("default-photo-url");
        cat = petRepository.save(cat);

        // Attach auction
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction();
        auction.setPet(cat);
        auction.setSellingUser(cat.getOwner());
        auction.setStartPrice(java.math.BigDecimal.ONE);
        auction.setHighestBid(java.math.BigDecimal.ONE);
        auction.setDescription("Cat auction");
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction.setPaymentDueDate(null);
        auction.setPaymentStatus(Payment_Status.UNPAID);
        auctionRepository.save(auction);

        UpdateCatRequest req = new UpdateCatRequest();
        req.setPetName("Mimi Updated");
        req.setPetAgeMonths(11);
        req.setPetSex(Sex.F);
        req.setPetWeight(3.8);
        req.setCatBreed("Tabby");
        req.setCatCoatLength(Coat_Length.LONG);
        req.setCatIndoorOnly(Indoor.NO);

        Long catId = cat.getPetId();

        assertThrows(InvalidStateException.class,
                () -> petService.updateCat(catId, ownerId, req));
    }

    // -------------------------------------------------------------------------
    // updatePetWhenAuctionLive
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updatePetWhenAuctionLive: updates limited pet fields when auction is LIVE")
    void updatePetWhenAuctionLive_updatesWhenLive() {
        // new pet
        Pet pet = new Pet();
        pet.setPetName("Auction Dog");
        pet.setPetAgeMonths(20);
        pet.setPetSex(Sex.M);
        pet.setPetWeight(11.0);
        pet.setPetCategory(Category.Dog);
        pet.setDogBreed("Beagle");
        pet.setDogSize(Size.MEDIUM);
        pet.setDogTemperament("Friendly");
        pet.setDogIsHypoallergenic(Allergy.UNKNOWN);
        pet.setOwner(userRepository.findById(ownerId).orElseThrow());
        pet.setPrimaryPhotoUrl("default-photo-url");
        pet = petRepository.save(pet);

        // LIVE auction
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction();
        auction.setPet(pet);
        auction.setSellingUser(pet.getOwner());
        auction.setStartPrice(java.math.BigDecimal.ONE);
        auction.setHighestBid(java.math.BigDecimal.ONE);
        auction.setDescription("Auction");
        auction.setStatus(Auction_Status.LIVE);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction.setEndTime(now.plusDays(1));
        auction.setPaymentDueDate(null);
        auction.setPaymentStatus(Payment_Status.UNPAID);
        auction = auctionRepository.save(auction);

        UpdatePetWhenAuctionLiveRequest req = new UpdatePetWhenAuctionLiveRequest();
        req.setPetName("Auction Dog Updated");
        req.setPetAgeMonths(21);
        req.setPetWeight(12.0);

        petService.updatePetWhenAuctionLive(pet.getPetId(), auction.getAuctionId(), req);

        Pet reloaded = petRepository.findById(pet.getPetId()).orElseThrow();
        assertThat(reloaded.getPetName()).isEqualTo("Auction Dog Updated");
        assertThat(reloaded.getPetAgeMonths()).isEqualTo(21);
        assertThat(reloaded.getPetWeight()).isEqualTo(12.0);
    }

    // -------------------------------------------------------------------------
    // changePetImage
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("changePetImage: updates primary photo URL when valid")
    void changePetImage_updatesUrl() {
        String newUrl = "https://cdn.example.com/pets/rex-new.jpg";

        petService.changePetImage(existingDogId, ownerId, newUrl);

        Pet reloaded = petRepository.findById(existingDogId).orElseThrow();
        assertThat(reloaded.getPrimaryPhotoUrl()).isEqualTo(newUrl);
    }

    @Test
    @Transactional
    @DisplayName("changePetImage: blank URL throws ValidationException")
    void changePetImage_blankUrl_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> petService.changePetImage(existingDogId, ownerId, "  "));
    }

    // -------------------------------------------------------------------------
    // getPetsByOwner
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("getPetsByOwner: returns pets owned by user")
    void getPetsByOwner_returnsOwnedPets() {
        var pets = petService.getPetsByOwner(ownerId);
        assertThat(pets).isNotEmpty();
        assertThat(pets.stream().map(PetDto::getOwnerId)).contains(ownerId);
    }
}