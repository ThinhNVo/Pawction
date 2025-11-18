package com.voti.pawction.dtos.response;

import com.voti.pawction.entities.pet.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetDto {
    private Long petId;
    private String petName;
    private String petType;
    private int petAgeMonths;
    private Sex petSex;
    private Double petWeight;
    private String primaryPhotoUrl;

    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy isHypoallergenic;

    private String catBreed;
    private Coat_Length coatLength;
    private Indoor catIndoorOnly;

}
