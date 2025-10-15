package com.voti.pawction.repositories.pet;

import com.voti.pawction.entities.pet.Pet;
import org.springframework.data.repository.CrudRepository;

public interface PetRepository extends CrudRepository<Pet, Long> {
}
