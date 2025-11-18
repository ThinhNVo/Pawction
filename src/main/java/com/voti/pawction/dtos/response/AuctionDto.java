package com.voti.pawction.dtos.response;

import com.voti.pawction.entities.auction.enums.Auction_Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuctionDto {
    Long auctionId;
    BigDecimal startPrice;
    BigDecimal highestBid;
    Auction_Status status;
    LocalDateTime createdAt;
    LocalDateTime endTime;
    LocalDateTime updatedAt;
    Long petId;
    Long sellingUserId;
}
