package com.voti.pawction.services.socket;

import com.voti.pawction.dtos.response.AuctionUpdateDto;
import com.voti.pawction.dtos.response.BidUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuctionUpdateService {
    private final SimpMessagingTemplate messagingTemplate;

    public void sendAuctionUpdate(Long auctionId, BigDecimal highestBid, int bidCount, BigDecimal userBidAmount, BigDecimal minNextBidAmount) {
        AuctionUpdateDto message = new AuctionUpdateDto(auctionId, highestBid, bidCount, userBidAmount, minNextBidAmount);

        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, message);
        messagingTemplate.convertAndSend("/topic/myAccount", message);
        messagingTemplate.convertAndSend("/topic/home", message);
    }

    public void sendBidUpdate(BidUpdateDto bidUpdateDto) {
        messagingTemplate.convertAndSend("/topic/bids/" + bidUpdateDto.getAuctionId(), bidUpdateDto);
    }

}
