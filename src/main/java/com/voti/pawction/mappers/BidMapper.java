package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.auction.Bid;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface BidMapper {
    BidDto toDto(Bid bid);

    Bid toEntity(BidDto dto);
}
