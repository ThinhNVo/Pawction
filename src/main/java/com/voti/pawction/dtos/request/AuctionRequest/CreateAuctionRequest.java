package com.voti.pawction.dtos.request.AuctionRequest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    private BigDecimal startPrice;
    private String description;
    private LocalDateTime endedAt;
}
