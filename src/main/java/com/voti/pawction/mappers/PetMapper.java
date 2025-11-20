package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.pet.Pet;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface PetMapper {
    PetDto toDto(Pet pet);
    Pet toEntity(PetDto dto);

}
