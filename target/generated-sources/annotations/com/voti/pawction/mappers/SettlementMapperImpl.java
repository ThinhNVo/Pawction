package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.SettlementDto;
import com.voti.pawction.entities.auction.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-03T23:29:44-0500",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class SettlementMapperImpl implements SettlementMapper {

    @Override
    public SettlementDto toDto(Auction auction) {
        if ( auction == null ) {
            return null;
        }

        Long auctionId = null;

        auctionId = auction.getAuctionId();

        Long winningUserId = null;
        BigDecimal finalBid = null;
        LocalDateTime paymentDueAt = null;

        SettlementDto settlementDto = new SettlementDto( auctionId, winningUserId, finalBid, paymentDueAt );

        return settlementDto;
    }

    @Override
    public Auction toEntity(SettlementDto dto) {
        if ( dto == null ) {
            return null;
        }

        Auction.AuctionBuilder auction = Auction.builder();

        auction.auctionId( dto.getAuctionId() );

        return auction.build();
    }
}
