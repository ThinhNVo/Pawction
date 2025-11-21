package com.voti.pawction.services.auction.impl;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.BidNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.InvalidBidException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.services.auction.AuctionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BiddingServiceInterface {
// ---------- Commands ----------
    /**
     * Places a bid on a LIVE auction on behalf of the given bidder.
     * <p>
     * Flow:
     * <ul>
     *   <li>Loads the bidder and auction with a row lock.</li>
     *   <li>Validates auction status and end time.</li>
     *   <li>Computes the required deposit hold amount via {@link AuctionService#requireAmount(Long)}.</li>
     *   <li>Checks that the bid amount is a valid increment over the current highest bid.</li>
     *   <li>Ensures the bidder has sufficient available funds and places a wallet hold.</li>
     *   <li>Persists the bid as {@link Bid_Status#WINNING}.</li>
     *   <li>Marks all other bids on this auction as {@link Bid_Status#OUTBID} in bulk.</li>
     *   <li>Updates the auction's {@code highestBid} and {@code winningUser}.</li>
     * </ul>
     *
     * @param bidderId  the id of the bidding user
     * @param auctionId the id of the auction to bid on
     * @param amount    proposed bid amount (must be positive)
     * @return the created bid as a DTO
     * @throws AuctionInvalidStateException if the auction is not LIVE or already ended
     * @throws InvalidBidException          if the bid amount is not a valid increment
     * @throws InvalidAmountException       if the bidder has insufficient funds for the required hold
     * @throws UserNotFoundException        if the bidder id is not found
     * @throws AuctionNotFoundException     if the auction id is not found
     */
    BidDto placeBid(Long bidderId, Long auctionId, BigDecimal amount);


    // ---------- Read helpers for orchestration ----------
    /**
     * Returns the current winning bid for an auction, if any.
     * <p>
     * "Winning" is defined as the highest amount with earliest bid time
     * (tie-breaker), as implemented by the repository query.
     *
     * @param auctionId the auction identifier
     * @return an {@link Optional} with the winning bid DTO, or empty if no bids exist
     * @throws AuctionNotFoundException if the auction id is not found
     */
    Optional<BidDto> getWinningBid(Long auctionId);

    /**
     * Returns the second-highest bid for an auction, if any.
     * <p>
     * This is typically used for fallback logic when the provisional winner
     * fails to complete payment and the platform offers the item to the
     * runner-up.
     *
     * @param auctionId the auction identifier
     * @return an {@link Optional} with the second-highest bid DTO, or empty if fewer than two bids exist
     * @throws AuctionNotFoundException if the auction id is not found
     */
    Optional<BidDto> getSecondHighestBid(Long auctionId);

    /**
     * Finalizes bid statuses when an auction is closed.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If no bids exist, does nothing.</li>
     *   <li>Marks the top bid as {@link Bid_Status#WON} if it was still WINNING.</li>
     *   <li>Marks all other bids on the auction as {@link Bid_Status#OUTBID}
     *       using a bulk update.</li>
     * </ul>
     * <p>
     * This method is idempotent and safe to call multiple times.
     *
     * @param auctionId the auction identifier
     * @throws AuctionNotFoundException if the auction id is not found
     * @throws BidNotFoundException     if the top bid cannot be reloaded unexpectedly
     */
    void finalizeBidsOnClose(Long auctionId);
}
