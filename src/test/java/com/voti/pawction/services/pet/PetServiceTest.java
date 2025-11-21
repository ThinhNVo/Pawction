package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.InvalidStateException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.services.auction.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PetService}.
 *
 * Uses JUnit 5 + Mockito with AAA pattern and mocks for all collaborators
 * (repositories, mapper, and {@link AuctionService}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetService unit tests")
class PetServiceTest {

    @Mock
    private PetMapper petMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private PetService petService;

    private User owner;
    private Pet pet;
    private PetDto petDto;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(1L);
        owner.setName("Owner");

        pet = new Pet();
        pet.setPetId(10L);
        pet.setOwner(owner);
        pet.setPetCategory(Category.Dog);

        petDto = mock(PetDto.class);
    }

    // ---------------------------------------------------------------------
    // registerDog
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("registerDog")
    class RegisterDogTests {

        @Test
        @DisplayName("successfully registers dog with valid request")
        void registerDog_validRequest_returnsDto() {
            // Arrange
            Long sellerId = 1L;
            RegisterDogRequest request = new RegisterDogRequest();
            request.setPetName("Buddy");
            request.setPetAgeMonths(12);
            request.setPetSex(Sex.M);
            request.setPetWeight(5.0);
            request.setPetCategory(Category.Dog);
            request.setDogBreed("Corgi");
            request.setDogSize(Size.SMALL);
            request.setDogTemperament("Friendly");
            request.setDogIsHypoallergenic(Allergy.NO);
            request.setPrimaryPhotoUrl("http://image/dog.jpg");

            when(userRepository.findById(sellerId))
                    .thenReturn(Optional.of(owner));
            when(petRepository.save(any(Pet.class)))
                    .thenAnswer(inv -> {
                        Pet p = inv.getArgument(0);
                        p.setPetId(10L);
                        return p;
                    });
            when(petMapper.toDto(any(Pet.class)))
                    .thenReturn(petDto);

            // Act
            PetDto result = petService.registerDog(sellerId, request);

            // Assert
            assertNotNull(result);

            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(petCaptor.capture());
            Pet saved = petCaptor.getValue();

            assertEquals("Buddy", saved.getPetName());
            assertEquals(Category.Dog, saved.getPetCategory());
            assertEquals("Corgi", saved.getDogBreed());
            assertEquals(Size.SMALL, saved.getDogSize());
            assertEquals("Friendly", saved.getDogTemperament());
            assertEquals(Allergy.NO, saved.getDogIsHypoallergenic());
            assertEquals("http://image/dog.jpg", saved.getPrimaryPhotoUrl());
            assertEquals(owner, saved.getOwner());
        }

        @Test
        @DisplayName("throws ValidationException when primary photo is missing")
        void registerDog_missingPrimaryPhoto_throwsValidationException() {
            // Arrange
            Long sellerId = 1L;
            RegisterDogRequest request = new RegisterDogRequest();
            request.setPetName("Buddy");
            request.setPetAgeMonths(12);
            request.setPetSex(Sex.M);
            request.setPetWeight(5.0);
            request.setPetCategory(Category.Dog);
            request.setDogBreed("Corgi");
            request.setDogSize(Size.SMALL);
            request.setDogTemperament("Friendly");
            request.setDogIsHypoallergenic(Allergy.NO);
            request.setPrimaryPhotoUrl(" "); // invalid

            when(userRepository.findById(sellerId))
                    .thenReturn(Optional.of(owner));

            // Act + Assert
            assertThrows(ValidationException.class,
                    () -> petService.registerDog(sellerId, request));

            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when seller does not exist")
        void registerDog_sellerNotFound_throwsUserNotFoundException() {
            // Arrange
            Long sellerId = 99L;
            RegisterDogRequest request = new RegisterDogRequest();
            request.setPetName("Buddy");
            request.setPetAgeMonths(12);
            request.setPetSex(Sex.M);
            request.setPetWeight(5.0);
            request.setPetCategory(Category.Dog);
            request.setDogBreed("Corgi");
            request.setDogSize(Size.SMALL);
            request.setDogTemperament("Friendly");
            request.setDogIsHypoallergenic(Allergy.NO);
            request.setPrimaryPhotoUrl("http://image/dog.jpg");

            when(userRepository.findById(sellerId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class,
                    () -> petService.registerDog(sellerId, request));

            verify(petRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // registerCat
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("registerCat")
    class RegisterCatTests {

        @Test
        @DisplayName("successfully registers cat with valid request")
        void registerCat_validRequest_returnsDto() {
            // Arrange
            Long sellerId = 1L;
            RegisterCatRequest request = new RegisterCatRequest();
            request.setPetName("Mimi");
            request.setPetAgeMonths(8);
            request.setPetSex(Sex.F);
            request.setPetWeight(3.5);
            request.setPetCategory(Category.Cat);
            request.setCatBreed("British Shorthair");
            request.setCatCoatLength(Coat_Length.SHORT);
            request.setCatIndoorOnly(Indoor.YES);
            request.setPrimaryPhotoUrl("http://image/cat.jpg");

            when(userRepository.findById(sellerId))
                    .thenReturn(Optional.of(owner));
            when(petRepository.save(any(Pet.class)))
                    .thenAnswer(inv -> {
                        Pet p = inv.getArgument(0);
                        p.setPetId(11L);
                        return p;
                    });
            when(petMapper.toDto(any(Pet.class)))
                    .thenReturn(petDto);

            // Act
            PetDto result = petService.registerCat(sellerId, request);

            // Assert
            assertNotNull(result);

            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(petCaptor.capture());
            Pet saved = petCaptor.getValue();

            assertEquals("Mimi", saved.getPetName());
            assertEquals(Category.Cat, saved.getPetCategory());
            assertEquals("British Shorthair", saved.getCatBreed());
            assertEquals(Coat_Length.SHORT, saved.getCatCoatLength());
            assertEquals(Indoor.YES, saved.getCatIndoorOnly());
        }

        @Test
        @DisplayName("throws ValidationException when primary photo is missing")
        void registerCat_missingPrimaryPhoto_throwsValidationException() {
            // Arrange
            Long sellerId = 1L;
            RegisterCatRequest request = new RegisterCatRequest();
            request.setPetName("Mimi");
            request.setPetAgeMonths(8);
            request.setPetSex(Sex.F);
            request.setPetWeight(3.5);
            request.setPetCategory(Category.Cat);
            request.setCatBreed("British Shorthair");
            request.setCatCoatLength(Coat_Length.SHORT);
            request.setCatIndoorOnly(Indoor.YES);
            request.setPrimaryPhotoUrl(null); // invalid

            when(userRepository.findById(sellerId))
                    .thenReturn(Optional.of(owner));

            // Act + Assert
            assertThrows(ValidationException.class,
                    () -> petService.registerCat(sellerId, request));

            verify(petRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // updateDog
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("updateDog")
    class UpdateDogTests {

        @Test
        @DisplayName("updates dog when seller owns pet and pet is not in auction")
        void updateDog_validInputs_updatesPet() {
            // Arrange
            Long petId = 10L;
            Long sellerId = 1L;
            pet.setAuction(null);

            UpdateDogRequest request = new UpdateDogRequest();
            request.setPetName("Buddy Updated");
            request.setPetAgeMonths(13);
            request.setPetSex(Sex.M);
            request.setPetWeight(6.0);
            request.setDogBreed("Corgi");
            request.setDogSize(Size.MEDIUM);
            request.setDogTemperament("Calm");
            request.setDogIsHypoallergenic(Allergy.NO);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class)))
                    .thenReturn(pet);
            when(petMapper.toDto(any(Pet.class)))
                    .thenReturn(petDto);

            // Act
            PetDto result = petService.updateDog(petId, sellerId, request);

            // Assert
            assertNotNull(result);
            assertEquals("Buddy Updated", pet.getPetName());
            assertEquals(13, pet.getPetAgeMonths());
            assertEquals(6.0, pet.getPetWeight());
            assertEquals("Corgi", pet.getDogBreed());
            assertEquals(Size.MEDIUM, pet.getDogSize());
            assertEquals("Calm", pet.getDogTemperament());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when seller does not own the pet")
        void updateDog_sellerNotOwner_throwsIllegalArgumentException() {
            // Arrange
            Long petId = 10L;
            Long sellerId = 2L; // not owner
            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet)); // getPetOrThrow used in checkOwnership

            UpdateDogRequest request = new UpdateDogRequest();

            // Act + Assert
            assertThrows(IllegalArgumentException.class,
                    () -> petService.updateDog(petId, sellerId, request));

            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidStateException when pet is locked in auction")
        void updateDog_petInAuction_throwsInvalidStateException() {
            // Arrange
            Long petId = 10L;
            Long sellerId = 1L;

            Auction auction = new Auction();
            pet.setAuction(auction);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));

            UpdateDogRequest request = new UpdateDogRequest();

            // Act + Assert
            assertThrows(InvalidStateException.class,
                    () -> petService.updateDog(petId, sellerId, request));

            verify(petRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // updatePetWhenAuctionLive
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("updatePetWhenAuctionLive")
    class UpdatePetWhenAuctionLiveTests {

        @Test
        @DisplayName("updates pet when auction is LIVE and pet is attached")
        void updatePetWhenAuctionLive_validInputs_updatesPet() {
            // Arrange
            Long petId = 10L;
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);
            auction.setPet(pet);

            UpdatePetWhenAuctionLiveRequest request = new UpdatePetWhenAuctionLiveRequest();
            request.setPetName("New Name");
            request.setPetAgeMonths(20);
            request.setPetWeight(7.5);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(auctionService.getAuctionOrThrow(auctionId))
                    .thenReturn(auction);
            when(petRepository.save(any(Pet.class)))
                    .thenReturn(pet);
            when(petMapper.toDto(any(Pet.class)))
                    .thenReturn(petDto);

            // Act
            PetDto result = petService.updatePetWhenAuctionLive(petId, auctionId, request);

            // Assert
            assertNotNull(result);
            assertEquals("New Name", pet.getPetName());
            assertEquals(20, pet.getPetAgeMonths());
            assertEquals(7.5, pet.getPetWeight());
        }

        @Test
        @DisplayName("throws InvalidStateException when auction is not LIVE")
        void updatePetWhenAuctionLive_auctionNotLive_throwsInvalidStateException() {
            // Arrange
            Long petId = 10L;
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.ENDED);
            auction.setPet(pet);

            UpdatePetWhenAuctionLiveRequest request = new UpdatePetWhenAuctionLiveRequest();
            request.setPetName("New Name");
            request.setPetAgeMonths(20);
            request.setPetWeight(7.5);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(auctionService.getAuctionOrThrow(auctionId))
                    .thenReturn(auction);

            // Act + Assert
            assertThrows(InvalidStateException.class,
                    () -> petService.updatePetWhenAuctionLive(petId, auctionId, request));

            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when pet is not attached to auction")
        void updatePetWhenAuctionLive_petNotAttached_throwsIllegalArgumentException() {
            // Arrange
            Long petId = 10L;
            Long auctionId = 100L;

            Auction auction = new Auction();
            auction.setAuctionId(auctionId);
            auction.setStatus(Auction_Status.LIVE);
            // auction.getPet() remains null

            UpdatePetWhenAuctionLiveRequest request = new UpdatePetWhenAuctionLiveRequest();
            request.setPetName("New Name");
            request.setPetAgeMonths(20);
            request.setPetWeight(7.5);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(auctionService.getAuctionOrThrow(auctionId))
                    .thenReturn(auction);

            // Act + Assert
            assertThrows(IllegalArgumentException.class,
                    () -> petService.updatePetWhenAuctionLive(petId, auctionId, request));

            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates AuctionNotFoundException when auction does not exist")
        void updatePetWhenAuctionLive_auctionNotFound_throwsAuctionNotFoundException() {
            // Arrange
            Long petId = 10L;
            Long auctionId = 100L;

            UpdatePetWhenAuctionLiveRequest request = new UpdatePetWhenAuctionLiveRequest();
            request.setPetName("New Name");
            request.setPetAgeMonths(20);
            request.setPetWeight(7.5);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(auctionService.getAuctionOrThrow(auctionId))
                    .thenThrow(new AuctionNotFoundException("Auction not found"));

            // Act + Assert
            assertThrows(AuctionNotFoundException.class,
                    () -> petService.updatePetWhenAuctionLive(petId, auctionId, request));
        }
    }

    // ---------------------------------------------------------------------
    // changePetImage
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("changePetImage")
    class ChangePetImageTests {

        @Test
        @DisplayName("changes pet image when seller owns pet and pet is not in auction")
        void changePetImage_validInputs_updatesUrl() {
            // Arrange
            Long petId = 10L;
            Long sellerId = 1L;
            pet.setAuction(null);
            pet.setPrimaryPhotoUrl("old");

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));
            when(petRepository.save(any(Pet.class)))
                    .thenReturn(pet);
            when(petMapper.toDto(any(Pet.class)))
                    .thenReturn(petDto);

            String newUrl = "http://image/new.jpg";

            // Act
            PetDto result = petService.changePetImage(petId, sellerId, newUrl);

            // Assert
            assertNotNull(result);
            assertEquals(newUrl, pet.getPrimaryPhotoUrl());
        }

        @Test
        @DisplayName("throws ValidationException when new image URL is blank")
        void changePetImage_blankUrl_throwsValidationException() {
            // Arrange
            Long petId = 10L;
            Long sellerId = 1L;
            pet.setAuction(null);

            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));

            // Act + Assert
            assertThrows(ValidationException.class,
                    () -> petService.changePetImage(petId, sellerId, "  "));

            verify(petRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // getPetOrThrow
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("getPetOrThrow")
    class GetPetOrThrowTests {

        @Test
        @DisplayName("returns pet when found")
        void getPetOrThrow_petExists_returnsPet() {
            // Arrange
            Long petId = 10L;
            when(petRepository.findById(petId))
                    .thenReturn(Optional.of(pet));

            // Act
            Pet result = petService.getPetOrThrow(petId);

            // Assert
            assertNotNull(result);
            assertEquals(petId, result.getPetId());
        }

        @Test
        @DisplayName("throws PetNotFoundException when pet does not exist")
        void getPetOrThrow_petMissing_throwsPetNotFoundException() {
            // Arrange
            Long petId = 10L;
            when(petRepository.findById(petId))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(PetNotFoundException.class,
                    () -> petService.getPetOrThrow(petId));
        }
    }
}