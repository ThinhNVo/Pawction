package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.SettlementDto;
import com.voti.pawction.entities.auction.Auction;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface SettlementMapper {
    SettlementDto toDto(Auction auction);

    Auction toEntity(SettlementDto dto);
}
