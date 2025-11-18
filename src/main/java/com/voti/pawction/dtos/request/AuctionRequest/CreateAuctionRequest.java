package com.voti.pawction.dtos.request.AuctionRequest;

import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    private BigDecimal startPrice;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}
