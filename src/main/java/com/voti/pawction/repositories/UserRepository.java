package com.voti.pawction.repositories;

import com.voti.pawction.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
