package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.PetRequest.RegisterPetRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PetServiceStub {
    private final PetRepository petRepository;
    private final PetMapper petMapper;
    private final AccountRepository accountRepository;

    //registerPet
    public PetDto registerPet(RegisterPetRequest request) {

        validatePetRequest(request);

        Pet pet = Pet.builder()
                .petName(request.getPetName())
                .petCategory(request.getPetCategory())
                .primaryPhotoUrl(request.getPrimaryPhotoUrl())
                .petAgeMonths(request.getPetAgeMonths())
                .petWeight(request.getPetWeight())
                .petSex(request.getPetSex())
                .dogBreed(request.getDogBreed())
                .dogSize(request.getDogSize())
                .dogTemperament(request.getDogTemperament())
                .dogIsHypoallergenic(request.getIsHypoallergenic())
                .catBreed(request.getCatBreed())
                .catCoatLength(request.getCoatLength())
                .catIndoorOnly(request.getCatIndoorOnly())
                .build();

        return petMapper.petToPetDto(petRepository.save(pet));
    }
    // --- Load Pet ---
    public Pet loadPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
    }


    public PetDto updatePet(Long petId, RegisterPetRequest request) {
        Pet pet = loadPet(petId);
        validatePetRequest(request);

        pet.setPetName(request.getPetName());
        pet.setPetCategory(request.getPetCategory());
        pet.setPrimaryPhotoUrl(request.getPrimaryPhotoUrl());
        pet.setPetAgeMonths(request.getPetAgeMonths());
        pet.setPetWeight(request.getPetWeight());
        pet.setPetSex(request.getPetSex());
        pet.setDogBreed(request.getDogBreed());
        pet.setDogSize(request.getDogSize());
        pet.setDogTemperament(request.getDogTemperament());
        pet.setDogIsHypoallergenic(request.getIsHypoallergenic());
        pet.setCatBreed(request.getCatBreed());
        pet.setCatCoatLength(request.getCoatLength());
        pet.setCatIndoorOnly(request.getCatIndoorOnly());

        return petMapper.petToPetDto(petRepository.save(pet));
    }

    public void validatePetRequest(RegisterPetRequest request) {


        if(request.getPetName() == null || request.getPetName().isBlank()){

            throw new IllegalArgumentException("Pet name is required");
        }

        if(request.getPetCategory() == null){
            throw new IllegalArgumentException("Pet Category is required");
        }

        if(request.getPrimaryPhotoUrl() == null || request.getPrimaryPhotoUrl().isBlank()){
            throw new IllegalArgumentException("Primary photo URL is required");
        }

        if (request.getPetAgeMonths() < 0) {
            throw new IllegalArgumentException("Pet age in months cannot be negative");
        }
        if (request.getPetWeight() != null && request.getPetWeight() <= 0) {
            throw new IllegalArgumentException("Pet weight must be positive if provided");
        }

        if (request.getPetSex() == null) {
            throw new IllegalArgumentException("Pet sex is required");}

        switch (request.getPetCategory()) {
            case Dog -> {
                if (request.getDogBreed() == null || request.getDogSize() == null) {
                    throw new IllegalArgumentException("Dog breed and size are required");
                }
            }
            case Cat -> {
                if (request.getCatBreed() == null || request.getCoatLength() == null) {
                    throw new IllegalArgumentException("Cat breed and coat length are required");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported pet category");
        }

    }

}
