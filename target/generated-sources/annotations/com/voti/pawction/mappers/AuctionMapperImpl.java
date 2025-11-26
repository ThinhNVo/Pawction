package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-25T19:11:25-0500",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class AuctionMapperImpl implements AuctionMapper {

    @Override
    public AuctionDto toDto(Auction auction) {
        if ( auction == null ) {
            return null;
        }

        Long auctionId = null;
        BigDecimal startPrice = null;
        BigDecimal highestBid = null;
        Auction_Status status = null;
        LocalDateTime createdAt = null;
        LocalDateTime endTime = null;
        LocalDateTime updatedAt = null;

        auctionId = auction.getAuctionId();
        startPrice = auction.getStartPrice();
        highestBid = auction.getHighestBid();
        status = auction.getStatus();
        createdAt = auction.getCreatedAt();
        endTime = auction.getEndTime();
        updatedAt = auction.getUpdatedAt();

        Long petId = null;
        Long sellingUserId = null;

        AuctionDto auctionDto = new AuctionDto( auctionId, startPrice, highestBid, status, createdAt, endTime, updatedAt, petId, sellingUserId );

        return auctionDto;
    }

    @Override
    public Auction toEntity(AuctionDto dto) {
        if ( dto == null ) {
            return null;
        }

        Auction.AuctionBuilder auction = Auction.builder();

        auction.auctionId( dto.getAuctionId() );
        auction.startPrice( dto.getStartPrice() );
        auction.highestBid( dto.getHighestBid() );
        auction.status( dto.getStatus() );
        auction.endTime( dto.getEndTime() );
        auction.createdAt( dto.getCreatedAt() );
        auction.updatedAt( dto.getUpdatedAt() );

        return auction.build();
    }
}
