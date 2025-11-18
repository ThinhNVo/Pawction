package com.voti.pawction.mappers;

import com.voti.pawction.dtos.request.PetRequest.RegisterPetRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.pet.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel="spring")

public interface PetMapper {
    PetDto petToPetDto(Pet pet);
    Pet toPetEntity(RegisterPetRequest request);
    Pet updatePetFromDto(PetDto petDto, @MappingTarget Pet pet);

}
