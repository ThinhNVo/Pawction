package com.voti.pawction.repositories.auction;

import com.voti.pawction.entities.auction.Auction;
import org.springframework.data.repository.CrudRepository;

public interface AuctionRepository extends CrudRepository<Auction, Long> {
}
