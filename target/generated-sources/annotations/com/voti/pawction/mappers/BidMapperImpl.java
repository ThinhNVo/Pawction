package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Bid_Status;
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
public class BidMapperImpl implements BidMapper {

    @Override
    public BidDto toDto(Bid bid) {
        if ( bid == null ) {
            return null;
        }

        Long bidId = null;
        BigDecimal amount = null;
        LocalDateTime bidTime = null;

        bidId = bid.getBidId();
        amount = bid.getAmount();
        bidTime = bid.getBidTime();

        Long bidderId = null;
        Long auctionId = null;
        Bid_Status status = null;

        BidDto bidDto = new BidDto( bidId, bidderId, auctionId, amount, status, bidTime );

        return bidDto;
    }

    @Override
    public Bid toEntity(BidDto dto) {
        if ( dto == null ) {
            return null;
        }

        Bid.BidBuilder bid = Bid.builder();

        bid.bidId( dto.getBidId() );
        bid.amount( dto.getAmount() );
        bid.bidTime( dto.getBidTime() );

        return bid.build();
    }
}
