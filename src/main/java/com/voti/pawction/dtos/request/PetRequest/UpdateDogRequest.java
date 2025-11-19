package com.voti.pawction.dtos.request.PetRequest;

import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;

@Data
public class UpdateDogRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petSex;
    private Double petWeight;
    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy dogIsHypoallergenic;
}