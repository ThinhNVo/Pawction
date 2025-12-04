package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.auction.Auction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface AuctionMapper {
    @Mapping(target = "petId", source = "pet.petId")
    @Mapping(target = "sellingUserId", source = "sellingUser.userId")
    AuctionDto toDto(Auction auction);

    Auction toEntity(AuctionDto dto);
}
