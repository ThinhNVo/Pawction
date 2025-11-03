package com.voti.pawction.repositories.wallet;

import com.voti.pawction.entities.wallet.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface AccountRepository extends CrudRepository<Account, Long> {


    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
   select a.balance - coalesce(sum(h.amount), 0)
   from Account a
   left join a.holds h on h.depositStatus = com.voti.pawction.entities.wallet.enums.Status.HELD
   where a.accountId = :accountId
    group by a.balance
""")
    BigDecimal computeAvailable(@Param("accountId") Long accountId);
}
