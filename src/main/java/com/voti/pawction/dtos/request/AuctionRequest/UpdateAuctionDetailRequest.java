package com.voti.pawction.dtos.request.AuctionRequest;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAuctionDetailRequest {
    private String description;
    private LocalDateTime endedAt;
    private LocalDateTime updatedAt;
}
