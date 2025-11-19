package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.request.PetRequest.*;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.PetNotFoundException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.repositories.wallet.TransactionRepository;
import com.voti.pawction.services.pet.impl.PetServiceInterface;
import com.voti.pawction.services.wallet.AccountServiceStub;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PetService implements PetServiceInterface {
    private final UserRepository userRepository;
    private final PetMapper petMapper;
    private final PetRepository petRepository;
    private final AuctionRepository auctionRepository;



    @Transactional
    public PetDto registerDog(Long sellerId, RegisterDogRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new UserNotFoundException("Seller not found"));

        validateDogRequest(request);

        Pet pet = Pet.builder()
                .petName(request.getPetName())
                .petCategory(Category.Dog)
                .primaryPhotoUrl(request.getPrimaryPhotoUrl())
                .petAgeMonths(request.getPetAgeMonths())
                .petWeight(request.getPetWeight())
                .petSex(request.getPetSex())
                .dogBreed(request.getDogBreed())
                .dogSize(request.getDogSize())
                .dogTemperament(request.getDogTemperament())
                .dogIsHypoallergenic(request.getDogIsHypoallergenic())
                .owner(seller) // enforce ownership
                .build();

        return petMapper.toDto(petRepository.save(pet));
    }

    @Transactional
    public PetDto registerCat(Long sellerId, RegisterCatRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new UserNotFoundException("Seller not found"));

        validateCatRequest(request);

        Pet pet = Pet.builder()
                .petName(request.getPetName())
                .petCategory(Category.Cat)
                .primaryPhotoUrl(request.getPrimaryPhotoUrl())
                .petAgeMonths(request.getPetAgeMonths())
                .petWeight(request.getPetWeight())
                .petSex(request.getPetSex())
                .catBreed(request.getCatBreed())
                .catCoatLength(request.getCatCoatLength())
                .catIndoorOnly(request.getCatIndoorOnly())
                .owner(seller)
                .build();

        return petMapper.toDto(petRepository.save(pet));
    }


    @Transactional
    public PetDto updateDog(Long petId, Long sellerId, UpdateDogRequest request) {
        Pet pet = enforceOwnership(petId, sellerId);
        enforceNotInAuction(pet);

        validateDogUpdate(request);

        pet.setPetName(request.getPetName());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());
        pet.setDogTemperament(request.getDogTemperament());

        return petMapper.toDto(petRepository.save(pet));
    }

    @Transactional
    public PetDto updateCat(Long petId, Long sellerId, UpdateCatRequest request) {
        Pet pet = enforceOwnership(petId, sellerId);
        enforceNotInAuction(pet);

        validateCatUpdate(request);

        pet.setPetName(request.getPetName());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());
        pet.setCatIndoorOnly(request.getCatIndoorOnly());

        return petMapper.toDto(petRepository.save(pet));
    }


    @Transactional
    public PetDto updatePetWhenAuctionLive(Long petId, Long auctionId, UpdatePetWhenAuctionLiveRequest request) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (!auction.getPet().equals(pet)) {
            throw new IllegalArgumentException("Pet not attached to this auction");
        }
        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new IllegalStateException("Auction is not live");
        }

        // Apply limited updates allowed during live auction
        pet.setPetName(request.getPetName());
        pet.setPetWeight(request.getPetWeight());

        return petMapper.toDto(petRepository.save(pet));
    }

    @Transactional
    public PetDto changePetImage(Long petId, Long sellerId, ChangePetImageRequest request) {
        Pet pet = enforceOwnership(petId, sellerId);
        enforceNotInAuction(pet);

        if (request.getNewPetImageUrl() == null || request.getNewPetImageUrl().isBlank()) {
            throw new ValidationException("New image URL is required");
        }
        // Apply image change logic (replace/set/remove)
        pet.setPrimaryPhotoUrl(request.getNewPetImageUrl());

        return petMapper.toDto(petRepository.save(pet));
    }

    @Transactional
    public Pet findById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));
    }


    // --- Helper methods ---
    private Pet enforceOwnership(Long petId, Long sellerId) {
        Pet pet = findById(petId);
        // Assuming Pet has an owner field (User or Account)
        if (pet.getAuction() != null && !pet.getAuction().getSellingUser().getUserId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller does not own this pet");
        }
        return pet;
    }



    private void enforceNotInAuction(Pet pet) {
        if (pet.getAuction() != null) {
            throw new IllegalStateException("Pet is locked in an auction");
        }
    }

    private void validateDogRequest(RegisterDogRequest request) {
        // dog-specific
        if (request.getDogBreed() == null || request.getDogSize() == null) {
            throw new ValidationException("Dog breed and size are required");
        }

    }

    private void validateCatRequest(RegisterCatRequest request) {
        // cat-specific validation
        if (request.getCatBreed() == null || request.getCatCoatLength() == null) {
            throw new ValidationException("Cat breed and coat length are required");
        }
    }

    private void validateDogUpdate(UpdateDogRequest request) {
        // dog update validation
        if (request.getPetWeight() != null && request.getPetWeight() <= 0) {
            throw new ValidationException("Pet weight must be positive");
        }
    }

    private void validateCatUpdate(UpdateCatRequest request) {
        // cat update validation
        if (request.getPetWeight() != null && request.getPetWeight() <= 0) {
            throw new ValidationException("Pet weight must be positive");
        }

    }
}
