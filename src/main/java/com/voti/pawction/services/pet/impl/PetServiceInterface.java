package com.voti.pawction.services.pet.impl;


import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;


public interface PetServiceInterface {
    /**
     * Registers a new dog under the specified seller. Implementations
     * Should include dog-specific attributes including primary image, then persist
     * the pet record and return its DTO representation.
     *
     * @param sellerId the seller (owner) identifier
     * @param request  the dog registration payload
     * @return the created pet as a DTO
     * @exception com.voti.pawction.exceptions.UserExceptions.UserNotFoundException
     *         if the seller does not exist
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation (e.g., required attributes missing)
     * @exception com.voti.pawction.exceptions.PetExceptions.ImageProcessingException
     *         if provided image(s) cannot be processed
     */
    PetDto registerDog(Long sellerId, RegisterDogRequest request);

    /**
     * Registers a new cat under the specified seller. Implementations should
     * include cat-specific attributes including primary image, then persist
     * the pet record and return its DTO representation.
     *
     * @param sellerId the seller (owner) identifier
     * @param request  the cat registration payload
     * @return the created pet as a DTO
     * @exception com.voti.pawction.exceptions.UserExceptions.UserNotFoundException
     *         if the seller does not exist
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation
     * @exception com.voti.pawction.exceptions.PetExceptions.ImageProcessingException
     *         if provided image(s) cannot be processed
     */
    PetDto registerCat(Long sellerId, RegisterCatRequest request);

    /**
     * Updates mutable attributes for a dog owned by the seller. Implementations
     * must enforce ownership and guard against illegal edits (e.g., when the pet
     * is Not in an Auction), applying only allowed changes.
     *
     * @param petId    the pet identifier
     * @param sellerId the owner (seller) identifier
     * @param request  the dog update payload
     * @return the updated pet as a DTO
     * @exception com.voti.pawction.exceptions.UserExceptions.UserNotFoundException
     *         if the pet or seller does not exist
     * @exception com.voti.pawction.exceptions.PetExceptions.InvalidStateException
     *         if the pet cannot be edited in its current state (e.g., listed/locked)
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation
     */
    PetDto updateDog(Long petId, Long sellerId, UpdateDogRequest request);

    /**
     * Updates mutable attributes for a cat owned by the seller. Implementations
     * must enforce ownership and guard against illegal edits (e.g., when the pet
     * is Not in an Auction), applying only allowed changes.
     *
     * @param petId    the pet identifier
     * @param sellerId the owner (seller) identifier
     * @param request  the cat update payload
     * @return the updated pet as a DTO
     * @exception com.voti.pawction.exceptions.UserExceptions.UserNotFoundException
     *         if the pet or seller does not exist
     * @exception com.voti.pawction.exceptions.PetExceptions.InvalidStateException
     *         if the pet cannot be edited in its current state (e.g., listed/locked)
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation
     * AuctionNotFound from the new commit, please make template but leave exception out to run
     */
    PetDto updateCat(Long petId, Long sellerId, UpdateCatRequest request);

    /**
     * Updates mutable attributes for a cat owned by the seller. Implementations
     * must enforce ownership and guard against illegal edits (e.g., when the pet
     * is reserved/listed by a LIVE auction), applying only allowed changes.
     *
     * @param petId    the pet identifier
     * @param auctionId The auction pet is listed on
     * @param request  the cat update payload
     * @return the updated pet as a DTO
     * @exception com.voti.pawction.exceptions.PetExceptions.InvalidStateException
     *         if the pet cannot be edited in its current state (e.g., listed/locked)
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation
     * AuctionNotFound from the new commit, please make template but leave exception out to run
     */
    PetDto updatePetWhenAuctionLive(Long petId, Long auctionId, UpdatePetWhenAuctionLiveRequest request);

    /**
     * Changes the primary image or gallery for a pet. Implementations should
     * validate ownership and apply image limits/format rules. If the pet is
     * locked by an active listing, the change may be rejected by policy.
     *
     * @param petId    the pet identifier
     * @param sellerId the owner (seller) identifier
     * @param request  the image change payload (replace/set/remove operations)
     * @return the updated pet as a DTO reflecting new imagery
     * @exception com.voti.pawction.exceptions.UserExceptions.UserNotFoundException
     *         if the pet or seller does not exist
     * @exception com.voti.pawction.exceptions.PetExceptions.InvalidStateException
     *         if imagery cannot be changed in the pet’s current state
     * @exception com.voti.pawction.exceptions.PetExceptions.ImageProcessingException
     *         if provided image(s) are invalid or cannot be processed
     * @exception com.voti.pawction.exceptions.PetExceptions.StorageException
     *         if persistence of image(s) fails (e.g., I/O, cloud storage)
     * @exception com.voti.pawction.exceptions.PetExceptions.ValidationException
     *         if request fields fail validation (e.g., unsupported MIME type/size)
     */
    PetDto changePetImage(Long petId, Long sellerId, ChangePetImageRequest request);

    // ---------- LookUp ----------
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
    Pet findById(Long petId);
}
