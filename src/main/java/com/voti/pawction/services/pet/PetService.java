package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.exceptions.PetExceptions.ImageProcessingException;
import com.voti.pawction.exceptions.PetExceptions.InvalidStateException;
import com.voti.pawction.exceptions.PetExceptions.StorageException;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.services.pet.impl.PetServiceInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@AllArgsConstructor
public class PetService implements PetServiceInterface {
    private final PetMapper petMapper;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AuctionRepository auctionRepository;


    /**
     * Registers a new dog under the specified seller.
     *
     * This method validates the common pet fields and dog-specific attributes,
     * then creates and persists a {@link Pet} entity of category {@link Category#Dog}
     * and returns its DTO representation.
     *
     * @param sellerId the seller (owner) identifier
     * @param request  the dog registration payload
     * @return the created pet as a DTO
     * @throws UserNotFoundException   if the seller does not exist
     * @throws ValidationException     if request fields fail validation
     *                                  (e.g., missing name, weight, breed, size, temperament, or flags)
     * @throws ImageProcessingException reserved for future use if image processing
     *                                  is added to this flow
     */
    @Transactional
    public PetDto registerDog(Long sellerId, RegisterDogRequest request) {
        var seller = getOwnerOrThrow(sellerId);

        validateCommonPetInfo(request.getPetName(), request.getPetAgeMonths(), request.getPetSex(),
                request.getPetWeight(), request.getPetCategory());

        validateDogInfo(request.getDogBreed(), request.getDogSize(),
                request.getDogTemperament(), request.getDogIsHypoallergenic());

        if (request.getPrimaryPhotoUrl() == null || request.getPrimaryPhotoUrl().isBlank()) {
            throw new ValidationException("Primary photo is required");
        }

        var pet = new Pet();
        pet.setPetName(request.getPetName());
        pet.setPetCategory(Category.Dog);
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());
        pet.setPetSex(request.getPetSex());
        pet.setDogBreed(request.getDogBreed());
        pet.setDogSize(request.getDogSize());
        pet.setDogTemperament(request.getDogTemperament());
        pet.setDogIsHypoallergenic(request.getDogIsHypoallergenic());
        pet.setOwner(seller);
        pet.setPrimaryPhotoUrl(request.getPrimaryPhotoUrl());

        return petMapper.toDto(petRepository.save(pet));
    }

    /**
     * Registers a new cat under the specified seller.
     *
     * This method validates the common pet fields and cat-specific attributes,
     * then creates and persists a {@link Pet} entity of category {@link Category#Cat}
     * and returns its DTO representation.
     *
     * @param sellerId the seller (owner) identifier
     * @param request  the cat registration payload
     * @return the created pet as a DTO
     * @throws UserNotFoundException   if the seller does not exist
     * @throws ValidationException     if request fields fail validation
     *                                  (e.g., missing name, weight, breed, coat length, or indoor flag)
     * @throws ImageProcessingException reserved for future use if image processing
     *                                  is added to this flow
     */
    @Transactional
    public PetDto registerCat(Long sellerId, RegisterCatRequest request) {
        User seller = getOwnerOrThrow(sellerId);

        validateCommonPetInfo(request.getPetName(), request.getPetAgeMonths(), request.getPetSex(),
                request.getPetWeight(), request.getPetCategory());

        validateCatInfo(request.getCatBreed(), request.getCatCoatLength(),
                request.getCatIndoorOnly());

        if (request.getPrimaryPhotoUrl() == null || request.getPrimaryPhotoUrl().isBlank()) {
            throw new ValidationException("Primary photo is required");
        }

        var pet = new Pet();
        pet.setPetName(request.getPetName());
        pet.setPetCategory(Category.Cat);
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());
        pet.setPetSex(request.getPetSex());
        pet.setCatBreed(request.getCatBreed());
        pet.setCatCoatLength(request.getCatCoatLength());
        pet.setCatIndoorOnly(request.getCatIndoorOnly());
        pet.setOwner(seller);
        pet.setPrimaryPhotoUrl(request.getPrimaryPhotoUrl());

        return petMapper.toDto(petRepository.save(pet));
    }

    /**
     * Updates mutable attributes for a dog owned by the seller.
     *
     * This method enforces ownership, ensures the pet is not currently attached
     * to an auction, validates the updated dog payload, and then applies the
     * allowed changes.
     *
     * @param petId    the pet identifier
     * @param sellerId the owner (seller) identifier
     * @param request  the dog update payload
     * @return the updated pet as a DTO
     * @throws PetNotFoundException   if the pet does not exist
     * @throws IllegalArgumentException if the seller does not own this pet
     * @throws InvalidStateException  if the pet is currently locked by an auction
     * @throws ValidationException    if request fields fail validation
     */
    @Transactional
    public PetDto updateDog(Long petId, Long sellerId, UpdateDogRequest request) {
        checkOwnership(petId, sellerId);

        enforceNotInAuction(petId);

        validateCommonPetInfo(request.getPetName(), request.getPetAgeMonths(), request.getPetSex(),
                request.getPetWeight(), Category.Dog);

        validateDogInfo(request.getDogBreed(), request.getDogSize(),
                request.getDogTemperament(), request.getDogIsHypoallergenic());


        var pet = getPetOrThrow(petId);
        pet.setPetName(request.getPetName());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetSex(request.getPetSex());
        pet.setPetWeight(request.getPetWeight());
        pet.setDogBreed(request.getDogBreed());
        pet.setDogSize(request.getDogSize());
        pet.setDogTemperament(request.getDogTemperament());
        pet.setDogIsHypoallergenic(request.getDogIsHypoallergenic());

        return petMapper.toDto(petRepository.save(pet));
    }

    /**
     * Updates mutable attributes for a cat owned by the seller.
     *
     * This method enforces ownership, ensures the pet is not currently attached
     * to an auction, validates the updated cat payload, and then applies the
     * allowed changes.
     *
     * @param petId    the pet identifier
     * @param sellerId the owner (seller) identifier
     * @param request  the cat update payload
     * @return the updated pet as a DTO
     * @throws PetNotFoundException   if the pet does not exist
     * @throws IllegalArgumentException if the seller does not own this pet
     * @throws InvalidStateException  if the pet is currently locked by an auction
     * @throws ValidationException    if request fields fail validation
     */
    @Transactional
    public PetDto updateCat(Long petId, Long sellerId, UpdateCatRequest request) {
        checkOwnership(petId, sellerId);

        enforceNotInAuction(petId);

        validateCommonPetInfo(request.getPetName(), request.getPetAgeMonths(), request.getPetSex(),
                request.getPetWeight(), Category.Cat);

        validateCatInfo(request.getCatBreed(), request.getCatCoatLength(),
                request.getCatIndoorOnly());

        var pet = getPetOrThrow(petId);
        pet.setPetName(request.getPetName());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetSex(request.getPetSex());
        pet.setPetWeight(request.getPetWeight());
        pet.setCatBreed(request.getCatBreed());
        pet.setCatCoatLength(request.getCatCoatLength());
        pet.setCatIndoorOnly(request.getCatIndoorOnly());

        return petMapper.toDto(petRepository.save(pet));
    }


    /**
     * Updates a limited subset of pet attributes while the pet is listed on a LIVE auction.
     *
     * This method verifies that the pet exists, that the given auction exists,
     * that the auction is associated with this pet, and that the auction status is
     * {@link Auction_Status#LIVE}. Only a restricted set of fields (such as name
     * and weight) may be changed while an auction is live.
     *
     * @param petId     the pet identifier
     * @param auctionId the auction on which the pet is listed
     * @param request   the update payload allowed during a live auction
     * @return the updated pet as a DTO
     * @throws PetNotFoundException       if the pet does not exist
     * @throws AuctionNotFoundException   if the auction does not exist
     * @throws InvalidStateException      if the auction is not live
     * @throws ValidationException        if request fields fail validation
     * @throws IllegalArgumentException   if the pet is not attached to the given auction
     */
    @Transactional
    public PetDto updatePetWhenAuctionLive(Long petId, Long auctionId, UpdatePetWhenAuctionLiveRequest request) {
        var pet = getPetOrThrow(petId);

        Auction auction = getAuctionOrThrow(auctionId);

        if (!Objects.equals(auction.getPet().getPetId(), petId)) {
            throw new IllegalArgumentException("Pet not attached to this auction");
        }
        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new InvalidStateException("Auction is not live");
        }

        if (request.getPetName() == null || request.getPetName().isBlank()) {
            throw new ValidationException("Pet name is required");
        }

        if (request.getPetAgeMonths() < 0) {
            throw new ValidationException("Pet age in months cannot be negative");
        }

        if (request.getPetWeight() == null || request.getPetWeight() <= 0) {
            throw new ValidationException("Pet weight must be greater than 0");
        }

        pet.setPetName(request.getPetName());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());

        return petMapper.toDto(petRepository.save(pet));
    }

    /**
     * Changes the primary image for a pet.
     *
     * This method enforces ownership, ensures the pet is not currently attached
     * to an auction, validates the new image URL, and then updates the primary
     * photo field on the pet.
     *
     * @param petId          the pet identifier
     * @param sellerId       the owner (seller) identifier
     * @param newPetImageUrl the new primary image URL to apply
     * @return the updated pet as a DTO reflecting the new image
     * @throws PetNotFoundException     if the pet does not exist
     * @throws IllegalArgumentException if the seller does not own this pet
     * @throws InvalidStateException    if imagery cannot be changed in the pet’s current state
     *                                  (e.g., when the pet is locked by an auction)
     * @throws ValidationException      if the new image URL is null or blank
     * @throws ImageProcessingException reserved for future use if image processing
     *                                  is added to this flow
     * @throws StorageException         reserved for future use if image storage
     *                                  involves external systems
     */
    @Transactional
    public PetDto changePetImage(Long petId, Long sellerId, String newPetImageUrl) {
        checkOwnership(petId, sellerId);

        enforceNotInAuction(petId);

        var pet = getPetOrThrow(petId);
        if (newPetImageUrl == null || newPetImageUrl.isBlank()) {
            throw new ValidationException("New image URL is required");
        }

        pet.setPrimaryPhotoUrl(newPetImageUrl);

        return petMapper.toDto(petRepository.save(pet));
    }

    // ------------Validation methods------------
    /**
     * Verifies that the given seller is the owner of the specified pet.
     *
     * This method loads the {@link Pet} by {@code petId} and compares its owner id
     * with the provided {@code sellerId}. It is intended as a guard for update or
     * delete operations that should only be allowed by the pet's owner.
     *
     * @param petId    the pet identifier
     * @param sellerId the expected owner (seller) identifier
     * @throws PetNotFoundException    if the pet does not exist
     * @throws IllegalArgumentException if the seller does not own this pet
     */
    private void checkOwnership(Long petId, Long sellerId) {
        var pet = getPetOrThrow(petId);

        if (!Objects.equals(pet.getOwner().getUserId(), sellerId)) {
            throw new IllegalArgumentException("Seller does not own this pet");
        }
    }

    /**
     * Ensures that the specified pet is not currently associated with an auction.
     *
     * This method loads the {@link Pet} by {@code petId} and checks whether it has
     * a non-null {@code auction} reference. It is typically used as a guard before
     * operations that must not be performed while the pet is listed or locked in
     * an auction (for example, destructive updates or deletion).
     *
     * @param petId the pet identifier
     * @throws PetNotFoundException  if the pet does not exist
     * @throws InvalidStateException if the pet is currently locked in an auction
     */
    private void enforceNotInAuction(long petId) {
        var pet = getPetOrThrow(petId);

        if (pet.getAuction() != null) {
            throw new InvalidStateException("Pet is locked in an auction");
        }
    }

    /**
     * Validates common pet attributes that are shared between cats and dogs.
     *
     * This method checks that the name, age, sex, weight, and category are present
     * and logically valid. It does not enforce any species-specific rules.
     *
     * @param petName      the pet's name; must be non-null and non-blank
     * @param petAgeMonths the pet's age in months; must be zero or positive
     * @param petSex       the pet's sex; must not be {@code null}
     * @param petWeight    the pet's weight; must be non-null and greater than 0
     * @param petCategory  the pet's category (e.g., {@link Category#Dog}, {@link Category#Cat});
     *                     must not be {@code null}
     * @throws ValidationException if any of the fields fail validation
     */
    private void validateCommonPetInfo( String petName, int petAgeMonths, Sex petSex, Double petWeight,
                                           Category petCategory) {
        if (petName == null || petName.isBlank()) {
            throw new ValidationException("Pet name is required");
        }

        if (petAgeMonths < 0) {
            throw new ValidationException("Pet age in months cannot be negative");
        }

        if (petSex == null) {
            throw new ValidationException("Pet sex is required");
        }

        if (petWeight == null || petWeight <= 0) {
            throw new ValidationException("Pet weight must be greater than 0");
        }

        if (petCategory == null) {
            throw new ValidationException("Pet category is required");
        }
    }

    /**
     * Validates dog-specific attributes for registration or update flows.
     *
     * This method assumes common pet fields have already been validated and focuses
     * only on the dog-specific properties.
     *
     * @param dogBreed          the dog's breed; must be non-null and non-blank
     * @param dogSize           the dog's size enum; must not be {@code null}
     * @param dogTemperament    a short description of temperament; must be non-null and non-blank
     * @param dogIsHypoallergenic indicates whether the dog is hypoallergenic; must not be {@code null}
     * @throws ValidationException if any of the dog-specific fields fail validation
     */
    private void validateDogInfo(String dogBreed, Size dogSize, String dogTemperament, Allergy dogIsHypoallergenic) {
        if (dogBreed == null || dogBreed.isBlank()) {
            throw new ValidationException("Dog breed is required");
        }

        if (dogSize == null) {
            throw new ValidationException("Dog size is required");
        }

        if (dogTemperament == null || dogTemperament.isBlank()) {
            throw new ValidationException("Dog temperament is required");
        }

        if (dogIsHypoallergenic == null) {
            throw new ValidationException("Dog hypoallergenic flag is required");
        }
    }

    /**
     * Validates cat-specific attributes for registration or update flows.
     * <p>
     * This method assumes common pet fields have already been validated and focuses
     * only on the cat-specific properties.
     *
     * @param catBreed      the cat's breed; must be non-null and non-blank
     * @param catCoatLength the cat's coat length enum; must not be {@code null}
     * @param catIndoorOnly flag indicating whether the cat is indoor-only; must not be {@code null}
     * @throws ValidationException if any of the cat-specific fields fail validation
     */
    private void validateCatInfo(String catBreed, Coat_Length catCoatLength, Indoor catIndoorOnly) {
        if (catBreed == null || catBreed.isBlank()) {
            throw new ValidationException("Cat breed is required");
        }

        if (catCoatLength == null) {
            throw new ValidationException("Cat coat length is required");
        }

        if (catIndoorOnly == null) {
            throw new ValidationException("Indoor-only flag is required");
        }
    }

    //------------lookup------------
    /**
     * Retrieves the domain entity for the given pet identifier. Intended for
     * internal orchestration where access to the aggregate/entity is required.
     * Controllers should prefer DTO-returning lookups to avoid leaking entities.
     *
     * @param petId the pet identifier
     * @return the {@code Pet} entity
     * @exception PetNotFoundException
     *         if the pet does not exist
     */
    private Pet getPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));
    }

    /**
     * Retrieves the domain entity for the given user identifier. Intended for
     * internal orchestration where access to the aggregate/entity is required.
     * Controllers should prefer DTO-returning lookups to avoid leaking entities.
     *
     * @param ownerId the owner identifier
     * @return the {@code User} entity
     * @exception UserNotFoundException
     *         if the user does not exist
     */
    private User getOwnerOrThrow(Long ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));
    }

    /**
     * Retrieves the domain entity for the given auction identifier. Intended for
     * internal orchestration where access to the aggregate/entity is required.
     * Controllers should prefer DTO-returning lookups to avoid leaking entities.
     *
     * @param auctionId the auction identifier
     * @return the {@code Auction} entity
     * @exception AuctionNotFoundException
     *         if the user does not exist
     */
    private Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Owner not found"));
    }
}
