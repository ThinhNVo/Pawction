package com.voti.pawction.dtos.request.AuctionRequest;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAuctionEndTimeRequest {
    private LocalDateTime newEndTime;
    private LocalDateTime updatedAt;
}
