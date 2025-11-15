package com.voti.pawction.services.auction.impl;

public interface BiddingServiceInterface {
    // -------- Commands (single-transaction orchestration) --------

    /**
     * Place a bid atomically:
     *  - SELECT auction ... FOR UPDATE; verify status=LIVE & increment/min rules
     *  - Compute required hold; ensure/adjust hold via AccountService
     *  - Persist new Bid (WINNING); mark previous leader OUTBID
     *  - Update auction: highestPrice, provisionalWinner
     *  - Notify RankingService & push WebSocket update
     * Idempotent on (auctionId, bidderId, clientBidId) if provided in request.
     */
    BidResultDto placeBid(PlaceBidRequest request);

    /**
     * Optional: retract a bid if policy allows (e.g., within N seconds and not the current leader).
     * Recomputes winner if needed.
     */
    void retract(Long auctionId, Long bidderId, Long bidId, String reason);

    // -------- Queries --------

    Optional<BidDto> getWinningBid(Long auctionId);

    /**
     * List all bids for a given auction (newest first by default).
     */
    Page<BidSummaryDto> listAuctionBids(Long auctionId, Pageable pageable);

    /**
     * List a user's bids across auctions.
     */
    Page<BidSummaryDto> listUserBids(Long userId, Pageable pageable);

    /**
     * Fetch a single bid.
     */
    Optional<BidDto> findById(Long bidId);

    // -------- Domain helpers (validation & amounts) --------

    /**
     * Validate min-increment and other auction rules for a proposed amount.
     * Returns true only if amount is strictly greater than current highest and meets increment policy.
     */
    boolean isValidIncrement(Long auctionId, BigDecimal proposedAmount);

    /**
     * Compute next valid minimum bid (highest + minIncrement or rule-based step).
     */
    BigDecimal nextMinimumBid(Long auctionId);

    /**
     * Compute the additional hold required to place 'proposedAmount' for this bidder,
     * considering existing active holds on the same auction (if refundable).
     */
    BigDecimal requiredHoldAmount(Long auctionId, Long bidderId, BigDecimal proposedAmount);

    /**
     * Check user/bidder eligibility (account status, KYC, seller cannot bid, bans, etc.).
     * Keep as a separate hook so controllers can preflight checks.
     */
    boolean isEligibleBidder(Long auctionId, Long bidderId);

    // -------- Maintenance / recovery hooks --------

    /**
     * Idempotently mark the latest bid as winning and others as OUTBID.
     * Useful after manual corrections or event replay.
     */
    void recomputeLeader(Long auctionId);

    /**
     * Transition all bids on an auction to a final state when auction ends:
     * current leader -> WINNING (final), others -> OUTBID (final).
     * Called from AuctionService during close/settlement.
     */
    void finalizeBidsOnClose(Long auctionId);
}
