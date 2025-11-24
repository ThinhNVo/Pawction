package com.voti.pawction.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SettlementDto {
    Long auctionId;
    Long winningUserId;
    BigDecimal finalBid;
    LocalDateTime paymentDueAt;
}
