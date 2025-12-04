package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.auction.Bid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface BidMapper {
    @Mapping(source = "user.userId", target = "bidderId")
    @Mapping(source = "auction.auctionId", target = "auctionId")
    BidDto toDto(Bid bid);

    Bid toEntity(BidDto dto);
}
