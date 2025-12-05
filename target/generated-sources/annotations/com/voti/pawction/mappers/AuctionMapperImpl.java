package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Payment_Status;
import com.voti.pawction.entities.pet.Pet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T18:57:30-0500",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class AuctionMapperImpl implements AuctionMapper {

    @Override
    public AuctionDto toDto(Auction auction) {
        if ( auction == null ) {
            return null;
        }

        Long petId = null;
        Long sellingUserId = null;
        Long auctionId = null;
        BigDecimal startPrice = null;
        BigDecimal highestBid = null;
        Auction_Status status = null;
        LocalDateTime createdAt = null;
        LocalDateTime endTime = null;
        String description = null;
        LocalDateTime updatedAt = null;
        Payment_Status paymentStatus = null;

        petId = auctionPetPetId( auction );
        sellingUserId = auctionSellingUserUserId( auction );
        auctionId = auction.getAuctionId();
        startPrice = auction.getStartPrice();
        highestBid = auction.getHighestBid();
        status = auction.getStatus();
        createdAt = auction.getCreatedAt();
        endTime = auction.getEndTime();
        description = auction.getDescription();
        updatedAt = auction.getUpdatedAt();
        paymentStatus = auction.getPaymentStatus();

        AuctionDto auctionDto = new AuctionDto( auctionId, startPrice, highestBid, status, createdAt, endTime, description, updatedAt, petId, sellingUserId, paymentStatus );

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
        auction.description( dto.getDescription() );
        auction.status( dto.getStatus() );
        auction.endTime( dto.getEndTime() );
        auction.createdAt( dto.getCreatedAt() );
        auction.updatedAt( dto.getUpdatedAt() );
        auction.paymentStatus( dto.getPaymentStatus() );

        return auction.build();
    }

    private Long auctionPetPetId(Auction auction) {
        Pet pet = auction.getPet();
        if ( pet == null ) {
            return null;
        }
        return pet.getPetId();
    }

    private Long auctionSellingUserUserId(Auction auction) {
        User sellingUser = auction.getSellingUser();
        if ( sellingUser == null ) {
            return null;
        }
        return sellingUser.getUserId();
    }
}
