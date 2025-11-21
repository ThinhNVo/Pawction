package com.voti.pawction.dtos.request.BiddingRequest;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBidRequest {
    BigDecimal amount;
}
