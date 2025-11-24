package com.voti.pawction.dtos.request.PetRequest;

import com.voti.pawction.entities.pet.enums.*;
import com.voti.pawction.services.storage.FileStorageService;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Data
public class RegisterPetRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petSex;
    private Double petWeight;
    private Category category; // Dog or Cat

    // Dog-specific
    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy dogIsHypoallergenic;

    // Cat-specific
    private String catBreed;
    private Coat_Length catCoatLength;
    private Indoor catIndoorOnly;

    private MultipartFile primaryPhoto;

    public RegisterDogRequest toDogRequest(FileStorageService fileStorageService) {
        RegisterDogRequest dogReq = new RegisterDogRequest();
        dogReq.setPetName(petName);
        dogReq.setPetAgeMonths(petAgeMonths);
        dogReq.setPetSex(petSex);
        dogReq.setPetWeight(petWeight);
        dogReq.setPetCategory(Category.Dog);
        dogReq.setDogBreed(dogBreed);
        dogReq.setDogSize(dogSize);
        dogReq.setDogTemperament(dogTemperament);
        dogReq.setDogIsHypoallergenic(dogIsHypoallergenic);
        if (primaryPhoto != null && !primaryPhoto.isEmpty()) {
            String storedPath = fileStorageService.store(primaryPhoto);
            dogReq.setPrimaryPhotoUrl(storedPath);
        }

        return dogReq;
    }

    public RegisterCatRequest toCatRequest(FileStorageService fileStorageService) {
        RegisterCatRequest catReq = new RegisterCatRequest();
        catReq.setPetName(petName);
        catReq.setPetAgeMonths(petAgeMonths);
        catReq.setPetSex(petSex);
        catReq.setPetWeight(petWeight);
        catReq.setPetCategory(Category.Cat);
        catReq.setCatBreed(catBreed);
        catReq.setCatCoatLength(catCoatLength);
        catReq.setCatIndoorOnly(catIndoorOnly);
        if (primaryPhoto != null && !primaryPhoto.isEmpty()) {
            String storedPath = fileStorageService.store(primaryPhoto);
            catReq.setPrimaryPhotoUrl(storedPath);
        }

        return catReq;
    }
}

