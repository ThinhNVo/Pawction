package com.voti.pawction.mappers;

import com.voti.pawction.dtos.response.PetDto;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.pet.enums.Allergy;
import com.voti.pawction.entities.pet.enums.Category;
import com.voti.pawction.entities.pet.enums.Coat_Length;
import com.voti.pawction.entities.pet.enums.Indoor;
import com.voti.pawction.entities.pet.enums.Sex;
import com.voti.pawction.entities.pet.enums.Size;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-21T19:54:27-0500",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class PetMapperImpl implements PetMapper {

    @Override
    public PetDto toDto(Pet pet) {
        if ( pet == null ) {
            return null;
        }

        Long petId = null;
        String petName = null;
        int petAgeMonths = 0;
        Sex petSex = null;
        Double petWeight = null;
        Category petCategory = null;
        String dogBreed = null;
        Size dogSize = null;
        String dogTemperament = null;
        Allergy dogIsHypoallergenic = null;
        String catBreed = null;
        Coat_Length catCoatLength = null;
        Indoor catIndoorOnly = null;
        String primaryPhotoUrl = null;

        petId = pet.getPetId();
        petName = pet.getPetName();
        petAgeMonths = pet.getPetAgeMonths();
        petSex = pet.getPetSex();
        petWeight = pet.getPetWeight();
        petCategory = pet.getPetCategory();
        dogBreed = pet.getDogBreed();
        dogSize = pet.getDogSize();
        dogTemperament = pet.getDogTemperament();
        dogIsHypoallergenic = pet.getDogIsHypoallergenic();
        catBreed = pet.getCatBreed();
        catCoatLength = pet.getCatCoatLength();
        catIndoorOnly = pet.getCatIndoorOnly();
        primaryPhotoUrl = pet.getPrimaryPhotoUrl();

        PetDto petDto = new PetDto( petId, petName, petAgeMonths, petSex, petWeight, petCategory, dogBreed, dogSize, dogTemperament, dogIsHypoallergenic, catBreed, catCoatLength, catIndoorOnly, primaryPhotoUrl );

        return petDto;
    }

    @Override
    public Pet toEntity(PetDto dto) {
        if ( dto == null ) {
            return null;
        }

        Pet.PetBuilder pet = Pet.builder();

        pet.petId( dto.getPetId() );
        pet.petName( dto.getPetName() );
        pet.petAgeMonths( dto.getPetAgeMonths() );
        pet.petSex( dto.getPetSex() );
        pet.petWeight( dto.getPetWeight() );
        pet.petCategory( dto.getPetCategory() );
        pet.dogBreed( dto.getDogBreed() );
        pet.dogSize( dto.getDogSize() );
        pet.dogTemperament( dto.getDogTemperament() );
        pet.dogIsHypoallergenic( dto.getDogIsHypoallergenic() );
        pet.catBreed( dto.getCatBreed() );
        pet.catCoatLength( dto.getCatCoatLength() );
        pet.catIndoorOnly( dto.getCatIndoorOnly() );
        pet.primaryPhotoUrl( dto.getPrimaryPhotoUrl() );

        return pet.build();
    }
}
