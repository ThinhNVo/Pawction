package com.voti.pawction.controllers;

import com.voti.pawction.repositories.auction.BidRepository;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class AuctionController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final BidRepository bidRepository;


}
