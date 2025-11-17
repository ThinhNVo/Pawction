package com.voti.pawction.services.auction.impl;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.auction.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AuctionServiceInterface {
    // -------- Creation / lifecycle --------

    /**
     * Create and immediately start a LIVE auction.
     * Highest bid is initialized to startPrice; status = LIVE.
     */
    AuctionDto create(Long sellingUserId, Long petId, CreateAuctionRequest request);

    /**
     * Update mutable fields (pet information, description and endTime).
     * Guard illegal transitions (e.g., changing startPrice after LIVE).
     */
    AuctionDto update(Long auctionId, Long petId, UpdateAuctionDetailRequest request);

    /**
     * Seller ends the auction early (if policy allows). Transitions LIVE -> SETTLE.
     * Triggers winner selection & settlement coordination.
     */
    AuctionDto settle(Long auctionId, Long sellerId, LocalDateTime endedAt);

    /**
     * Seller cancels the auction. Allowed only before first valid bid (policy).
     * Transitions LIVE -> CANCELED. Notifies watchers/bidders.
     */
    void cancel(Long auctionId, Long sellerId, String reason);

    // -------- Scheduler / expiry handling --------

    /**
     * Close all auctions whose endTime <= now and are still LIVE.
     * Sets status=ENDED, picks provisional winner, sets payment deadline,
     * and coordinates with SettlementService. Returns number processed.
     */
    int closeExpiredAuctions();

    /**
     * Idempotent close for one auction (used by scheduler and manual retry).
     */
    Optional<AuctionDto> closeIfExpired(Long auctionId);

    // -------- Queries --------

    /**
     * Find a single auction with full details.
     */
    Auction findById(Long auctionId);

    // -------- Domain helpers (bidding rules) --------

    /**
     * Amount a bidder must have available (balance or hold) to place a bid of 'bidAmount'.
     * Typically equals bidAmount + required deposit/fee, minus existing refundable hold.
     */
    BigDecimal requiredAmountForBid(Long auctionId, BigDecimal bidAmount, Long bidderId);

    /**
     * Validate min-increment rule given the current highest bid.
     */
    boolean isValidIncrement(Long auctionId, BigDecimal proposedBid);

    /**
     * Compute the next valid minimum bid given current highest bid & increment policy.
     */
    BigDecimal nextMinimumBid(Long auctionId);

    // -------- Settlement coordination hooks --------

    /**
     * Called after ENDED to coordinate with SettlementService:
     * decide winner (if any), create payment deadline/hold instructions, etc.
     * Safe to call multiple times; must be idempotent.
     */
    AuctionDto end(Long auctionId);
}

