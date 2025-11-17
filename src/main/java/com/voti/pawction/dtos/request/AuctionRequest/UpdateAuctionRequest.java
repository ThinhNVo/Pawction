package com.voti.pawction.dtos.request.AuctionRequest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateAuctionRequest {
    private BigDecimal highestPrice;
    private LocalDateTime updatedAt;
    private LocalDateTime endedAt;
}
