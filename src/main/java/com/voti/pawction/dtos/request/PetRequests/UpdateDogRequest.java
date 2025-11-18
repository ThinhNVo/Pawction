package com.voti.pawction.dtos.request.PetRequests;

import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;

@Data
public class UpdateDogRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petGender;
    private Double petWeight;
    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy dogIsHypoallergenic;
}
