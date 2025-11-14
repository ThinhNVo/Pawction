package com.voti.pawction.dtos.request.PetRequests;


import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;

@Data
public class RegisterCatRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petGender;
    private Double petWeight;
    private Category petCategory;
    private String catBreed;
    private Coat_Length catCoatLength;
    private Indoor catIndoorOnly;
    private String primaryPhotoUrl;
}
