package com.voti.pawction.services.auction.impl;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionEndTimeRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdatePetWhenAuctionLiveRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.InvalidAuctionException;
import com.voti.pawction.exceptions.PetNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AuctionServiceInterface {
    // -------- Creation / lifecycle --------

    /**
     * Create and immediately start a LIVE auction.
     * Initializes highestBid to startPrice and sets endTime from the request.
     *
     * @param sellingUserId seller's user id
     * @param petId pet id to list
     * @param request payload with startPrice, description, endTime
     * @return created auction as DTO
     * @throws InvalidAmountException if startPrice <= 0
     * @throws InvalidAuctionException if description missing/blank or endTime invalid
     * @throws UserNotFoundException if seller not found
     * @throws PetNotFoundException if pet not found
     */
    AuctionDto create(Long sellingUserId, Long petId, CreateAuctionRequest request);

    /**
     * Update mutable auction details (currently description) while LIVE.
     *
     * @param auctionId auction identifier
     * @param request new description
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if description missing/blank
     */
    AuctionDto updateAuctionDetail(Long auctionId, UpdateAuctionDetailRequest request);

    /**
     * Update the auction end time while LIVE.
     * Business rule: new end time must be in the future and after creation;
     * optionally within a configured window.
     *
     * @param auctionId auction identifier
     * @param request wrapper for new end time
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if new end time invalid
     */
    AuctionDto updateAuctionEndTime(Long auctionId, UpdateAuctionEndTimeRequest request);

    /**
     * Update limited pet info during a LIVE auction (seller-only).
     *
     * @param auctionId auction identifier
     * @param sellerId authenticated seller id (must match auction.sellingUser.id)
     * @param request allowable pet fields to update during a live listing
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws IllegalArgumentException if caller is not the seller
     */
    AuctionDto updateAuctionPetInfo(Long auctionId, Long sellerId, UpdatePetWhenAuctionLiveRequest request);

    /**
     * Seller ends the auction early.
     * Transitions LIVE -> ENDED immediately and then delegates to {@link #end(Long)} for post-close orchestration.
     * Idempotent: if already ENDED, simply delegates to {@code end()}.
     *
     * @param auctionId Auction ID
     * @return ended auction DTO (post-close state applied)
     * @throws AuctionInvalidStateException if auction is CANCELED or already SETTLED
     */
    AuctionDto endEarly(Long auctionId);

    /**
     * Post-close orchestration for an auction (idempotent).
     *
     * <p>This method ensures the auction is in the LIVE or SETTLE state, then performs the
     * following post-close logic:
     * <ul>
     *   <li>If the auction is still LIVE, it is transitioned to ENDED.</li>
     *   <li>If the auction is still SETTLED, it is transitioned to ENDED.</li>
     *   <li>Determines if there is a winning bid using {@code BiddingService}.</li>
     *   <li>If no bids are present → delegates to {@code settlementService.noWinner(auctionId)}
     *       to release holds and finalize the auction.</li>
     *   <li>If there is a winner:
     *       <ul>
     *         <li>Stamps {@code provisionalWinnerUserId} and {@code finalPrice}.</li>
     *         <li>If no {@code paymentDueAt} is set, assigns a payment deadline
     *             (e.g., now + 24h, or via configuration).</li>
     *         <li>Delegates to {@code settlementService.begin(...)} to initiate the
     *             settlement workflow.</li>
     *       </ul>
     *   </li>
     *   <li>Notifies {@code rankingService.onAuctionClosed(...)} for leaderboard/UI updates.</li>
     * </ul>
     *
     * <p>This method is safe to call multiple times — it checks the auction state and only applies
     * missing post-close operations, making it suitable for both interactive calls and scheduler retries.</p>
     *
     * @param auctionId the ID of the auction to finalize
     * @return the resulting {@link AuctionDto}, reflecting ENDED state and any winner/payment metadata
     * @throws AuctionInvalidStateException if the auction is CANCELED or already ENDED
     */
    AuctionDto end(Long auctionId);

    /**
     * Cancel a LIVE auction before the first valid bid.
     * Transitions LIVE -> CANCELED and releases all holds via SettlementService.
     *
     * @param auctionId auction identifier
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if a first bid already occurred
     */
    void cancel(Long auctionId);

    // -------- Scheduler / expiry handling --------

    /**
     * Close all auctions whose endTime <= now and are still LIVE.
     * For each candidate, flips to ENDED (with locking) and delegates to {@link #end(Long)}.
     *
     * @return number of auctions processed
     */
    int closeExpiredAuctions();

    /**
     * Close a single auction if it has expired, then delegate post-close orchestration to {@link #end(Long)}.
     * This method acquires a PESSIMISTIC_WRITE lock on the auction row to prevent
     * concurrent closes/bids, flips LIVE -&gt; ENDED when {@code endTime &le nowNY},
     * stamps {@code updatedAt} (and {@code paymentDueAt} if missing), persists the change,
     * and finally calls {@link #end(Long)} to perform winner selection and settlement coordination.
     *
     * @param auctionId    the auction id to evaluate
     * @param nowNY the current timestamp in application time zone (e.g., America/New_York)
     * @return {@code true} if the auction was transitioned from LIVE to ENDED and {@code end(id)} was invoked;
     *         {@code false} if the auction was not LIVE or not yet expired
     */
    boolean closeOneIfExpired(Long auctionId, LocalDateTime nowNY);

    /**
     * Compute the next minimum bid (no min-increment policy).
     * Rule: highestBid + 1 unit (adjust if you support sub-unit currency).
     *
     * @param auctionId auction identifier
     * @return next minimum allowed bid
     */
    BigDecimal nextMinimumBid(Long auctionId);
}

