package com.voti.pawction.repositories.wallet;

import com.voti.pawction.entities.wallet.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {
}
