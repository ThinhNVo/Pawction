package com.voti.pawction.dtos.request;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.PetRequest.RegisterPetRequest;
import lombok.Data;

@Data
public class RegisterPetAndAuctionRequest {
    private RegisterPetRequest petRequest;
    private CreateAuctionRequest auctionRequest;
}
