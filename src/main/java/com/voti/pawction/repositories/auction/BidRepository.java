package com.voti.pawction.repositories.auction;

import com.voti.pawction.entities.auction.Bid;
import org.springframework.data.repository.CrudRepository;

public interface BidRepository extends CrudRepository<Bid, Long> {
}
