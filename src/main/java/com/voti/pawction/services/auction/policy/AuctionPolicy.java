package com.voti.pawction.services.auction.policy;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.repositories.auction.AuctionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@AllArgsConstructor
public class AuctionPolicy {
    private final AuctionRepository auctionRepository;

    /**
     * Compute the required deposit/hold amount for a given auction based on its start price.
     * Tiered policy: <= 50 => 5; <= 100 => 10; <= 500 => 25; else 50.
     *
     * @param auctionId auction identifier
     * @return required hold amount
     */
    public BigDecimal requireAmount(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);

        BigDecimal requiredAmount = BigDecimal.ZERO;

        var startPrice = auction.getStartPrice();

        if (startPrice.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return BigDecimal.valueOf(5);
        }

        if (startPrice.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return BigDecimal.valueOf(10);
        }

        if (startPrice.compareTo(BigDecimal.valueOf(500)) <= 0) {
            return BigDecimal.valueOf(25);
        }

        return BigDecimal.valueOf(50);
    }

    /**
     * Validate a proposed bid against the current highest bid (no min-increment policy).
     * Rule: proposed must be strictly greater than the current highest.
     *
     * @param auctionId auction identifier
     * @param proposedBid bid amount to validate (must be positive)
     * @return true if proposedBid > highestBid; false otherwise
     */
    public boolean isValidIncrement(Long auctionId, BigDecimal proposedBid) {
        var auction = getAuctionOrThrow(auctionId);

        Objects.requireNonNull(proposedBid, "amount");
        if (proposedBid.signum() <= 0) throw new InvalidAmountException("amount must be larger than 0");

        return proposedBid.compareTo(auction.getHighestBid()
                .add(BigDecimal.ONE)) >= 0;
    }

    /**
     * Fetches an auction by id or throws if not found.
     *
     * @param auctionId the account identifier
     * @return the account entity
     * @throws AuctionNotFoundException if the account doesn't exist
     */
    public Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(()-> new AuctionNotFoundException("Auction not found by id: " + auctionId));
    }
}
