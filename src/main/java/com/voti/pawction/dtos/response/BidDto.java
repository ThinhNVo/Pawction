package com.voti.pawction.dtos.response;

import com.voti.pawction.entities.auction.enums.Bid_Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BidDto {
    Long bidId;
    Long bidderId;
    Long auctionId;
    BigDecimal amount;
    Bid_Status status;
    LocalDateTime bidTime;
}
