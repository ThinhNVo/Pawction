package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.pet.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface PetMapper {
    //new
    @Mapping(source = "owner.userId", target = "ownerId")
    PetDto toDto(Pet pet);

    //new
    @Mapping(source = "ownerId", target = "owner.userId")
    Pet toEntity(PetDto dto);

}
