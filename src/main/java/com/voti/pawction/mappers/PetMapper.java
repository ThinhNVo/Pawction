package com.voti.pawction.mappers;

import com.voti.pawction.dtos.request.CreatePetRequest;
import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.pet.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import javax.swing.*;

@Mapper(componentModel = "spring")
public interface PetMapper {
    PetDto petToPetDto(Pet pet);
    Pet toPetEntity(CreatePetRequest request);
    Pet updatePetFromDto(PetDto petDto, @MappingTarget Pet pet);

}
