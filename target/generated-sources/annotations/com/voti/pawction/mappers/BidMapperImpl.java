package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Bid_Status;
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
public class BidMapperImpl implements BidMapper {

    @Override
    public BidDto toDto(Bid bid) {
        if ( bid == null ) {
            return null;
        }

        Long bidderId = null;
        Long auctionId = null;
        Long bidId = null;
        BigDecimal amount = null;
        LocalDateTime bidTime = null;

        bidderId = bidUserUserId( bid );
        auctionId = bidAuctionAuctionId( bid );
        bidId = bid.getBidId();
        amount = bid.getAmount();
        bidTime = bid.getBidTime();

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

        bid.user( bidDtoToUser( dto ) );
        bid.bidId( dto.getBidId() );
        bid.amount( dto.getAmount() );
        bid.bidTime( dto.getBidTime() );

        return bid.build();
    }

    private Long bidUserUserId(Bid bid) {
        User user = bid.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUserId();
    }

    private Long bidAuctionAuctionId(Bid bid) {
        Auction auction = bid.getAuction();
        if ( auction == null ) {
            return null;
        }
        return auction.getAuctionId();
    }

    protected User bidDtoToUser(BidDto bidDto) {
        if ( bidDto == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.userId( bidDto.getBidderId() );

        return user.build();
    }
}
