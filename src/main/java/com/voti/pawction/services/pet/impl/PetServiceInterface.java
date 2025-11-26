package com.voti.pawction.services.pet.impl;


import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.*;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.entities.pet.Pet;

import java.util.List;


public interface PetServiceInterface {
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
    PetDto registerDog(Long sellerId, RegisterDogRequest request);

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
    PetDto registerCat(Long sellerId, RegisterCatRequest request);

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
    PetDto updateDog(Long petId, Long sellerId, UpdateDogRequest request);

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
    PetDto updateCat(Long petId, Long sellerId, UpdateCatRequest request);

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
    PetDto updatePetWhenAuctionLive(Long petId, Long auctionId, UpdatePetWhenAuctionLiveRequest request);

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
    PetDto changePetImage(Long petId, Long sellerId, String newPetImageUrl);


    List<PetDto> getPetsByOwner(Long ownerId);
}
