package com.voti.pawction.dtos.request.PetRequests;

import com.voti.pawction.entities.pet.enums.Sex;
import lombok.Data;

@Data
public class UpdatePetWhenAuctionLiveRequest {
    private String petName;
    private int petAgeMonths;
    private Double petWeight;
}
