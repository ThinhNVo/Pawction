package com.voti.pawction.dtos.request.PetRequest;

import com.voti.pawction.entities.pet.enums.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RegisterDogRequest {
    private String petName;
    private int petAgeMonths;
    private Sex petSex;
    private Double petWeight;
    private Category petCategory;
    private String dogBreed;
    private Size dogSize;
    private String dogTemperament;
    private Allergy dogIsHypoallergenic;
    private MultipartFile primaryPhoto;
}