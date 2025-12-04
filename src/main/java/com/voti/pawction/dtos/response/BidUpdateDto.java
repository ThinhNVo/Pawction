package com.voti.pawction.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateDto {
    private Long bidId;
    private Long auctionId;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean isWinning;
}
