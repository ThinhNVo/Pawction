package com.voti.pawction.repositories.pet;

import com.voti.pawction.entities.pet.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerUserId(Long userId);


}
