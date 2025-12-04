package com.voti.pawction.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionUpdateDto {
    private Long auctionId;
    private BigDecimal highestBid;
    private int bidCount;
    private BigDecimal userBidAmount;
    private BigDecimal minNextBidAmount;
}
