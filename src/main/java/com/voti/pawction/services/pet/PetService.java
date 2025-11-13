package com.voti.pawction.services.pet;

import com.voti.pawction.dtos.request.CreatePetRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.mappers.PetMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.repositories.wallet.TransactionRepository;
import com.voti.pawction.services.wallet.AccountServiceStub;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PetService {
    private final UserRepository userRepository;
    private final PetMapper petMapper;
    private final PetRepository petRepository;


    @Transactional
    //Create pet and validate it
    public PetDto registerPet(Long userId, CreatePetRequest request) {


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

        Pet pet = petMapper.toPetEntity(request);

        return petMapper.petToPetDto(petRepository.save(pet));
    }

    public void updatePet(Long id, CreatePetRequest request) {
        Pet pet = petRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pet not found"));


        petRepository.save(pet);
    }

}
