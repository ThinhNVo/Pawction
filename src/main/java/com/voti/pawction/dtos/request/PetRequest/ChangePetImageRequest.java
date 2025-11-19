package com.voti.pawction.dtos.request.PetRequest;

import lombok.Data;

@Data
public class ChangePetImageRequest {
    private String oldPetImageUrl;
    private String newPetImageUrl;
}