package com.voti.pawction.repositories.wallet;

import com.voti.pawction.entities.wallet.Transaction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
}
