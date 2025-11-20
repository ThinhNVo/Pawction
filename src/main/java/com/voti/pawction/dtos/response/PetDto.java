package com.voti.pawction.dtos.response;

import com.voti.pawction.entities.pet.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetDto {
    private Long petId;
    private String petName;
    private int petAgeMonths;
    private Sex petSex;
    private Double petWeight;
    private Category petCategory;
    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy dogIsHypoallergenic;
    private String catBreed;
    private Coat_Length catCoatLength;
    private Indoor catIndoorOnly;
    private String primaryPhotoUrl;
}
