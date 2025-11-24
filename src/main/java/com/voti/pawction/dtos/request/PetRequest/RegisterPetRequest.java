package com.voti.pawction.dtos.request.PetRequest;

import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;

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

    private String primaryPhotoUrl;

    public RegisterDogRequest toDogRequest() {
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
        dogReq.setPrimaryPhotoUrl(primaryPhotoUrl);
        return dogReq;
    }

    public RegisterCatRequest toCatRequest() {
        RegisterCatRequest catReq = new RegisterCatRequest();
        catReq.setPetName(petName);
        catReq.setPetAgeMonths(petAgeMonths);
        catReq.setPetSex(petSex);
        catReq.setPetWeight(petWeight);
        catReq.setPetCategory(Category.Cat);
        catReq.setCatBreed(catBreed);
        catReq.setCatCoatLength(catCoatLength);
        catReq.setCatIndoorOnly(catIndoorOnly);
        catReq.setPrimaryPhotoUrl(primaryPhotoUrl);
        return catReq;
    }
}

