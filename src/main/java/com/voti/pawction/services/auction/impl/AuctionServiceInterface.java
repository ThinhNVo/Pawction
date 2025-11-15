package com.voti.pawction.services.auction.impl;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.response.AuctionDto;

import java.time.LocalDateTime;

public interface AuctionServiceInterface {
    // -------- Creation / lifecycle --------

    /**
     * Create and immediately start a LIVE auction.
     * Highest bid is initialized to startPrice; status = LIVE.
     */
    AuctionDto create(Long sellingUserId, Long petId, CreateAuctionRequest request);

    /**
     * Update mutable fields (title, description, endTime, minIncrement, etc.).
     * Guard illegal transitions (e.g., changing startPrice after LIVE).
     */
    AuctionDto update(UpdateAuctionRequest request);

    /**
     * Seller ends the auction early (if policy allows). Transitions LIVE -> ENDED.
     * Triggers winner selection & settlement coordination.
     */
    AuctionOutcomeDto end(Long auctionId, Long sellerId, LocalDateTime endedAt);

    /**
     * Seller cancels the auction. Allowed only before first valid bid (policy).
     * Transitions DRAFT/LIVE -> CANCELED. Notifies watchers/bidders.
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
    Optional<AuctionOutcomeDto> closeIfExpired(Long auctionId);

    // -------- Queries --------

    /**
     * Find a single auction with full details.
     */
    Optional<AuctionDto> findById(Long auctionId);

    /**
     * Active auctions with filters (category, text, price range, seller, etc.).
     * Only returns LIVE by default unless status is provided in the filter.
     */
    Page<AuctionSummaryDto> search(AuctionQuery filter, Pageable pageable);

    Page<AuctionSummaryDto> listBySeller(Long sellerId, Pageable pageable);

    Page<AuctionSummaryDto> listByStatus(AuctionStatus status, Pageable pageable);

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
    AuctionOutcomeDto settle(Long auctionId);
}

