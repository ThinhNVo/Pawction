package com.voti.pawction.dtos.request.PetRequests;

import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;

@Data
public class UpdateCatRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petGender;
    private Double petWeight;
    private String catBreed;
    private Coat_Length catCoatLength;
    private Indoor catIndoorOnly;
}
