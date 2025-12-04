package com.voti.pawction.repositories.wallet;

import com.voti.pawction.entities.wallet.DepositHold;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface DepositHoldRepository extends CrudRepository<DepositHold, Long> {
    Optional<DepositHold> findByAccountAccountIdAndAuctionAuctionId(Long accountId, Long auctionId);
}
