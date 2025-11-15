package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.auction.Auction;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface AuctionMapper {
    AuctionDto toDto(Auction auction);

    Auction toEntity(AuctionDto dto);
}
