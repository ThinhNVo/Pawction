package com.voti.pawction.services.auction.impl;


import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.SettlementDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SettlementServiceInterface {
    // ---------- Kickoff when auction closes with a winner ----------

    /**
     * Start settlement for a closed auction with a winner.
     * Creates/updates a settlement record in PENDING_PAYMENT state.
     */
    SettlementDto begin(Long auctionId,
                        Long winnerUserId,
                        LocalDateTime paymentDueAt);

    /**
     * Handle the case where an auction ends with no winner or only 1 winner but didn't pay
     *
     */
    SettlementDto noWinner(Long auctionId);

    // ---------- Payment lifecycle ----------

    /**
     * Record a payment attempt for this auction.
     * Idempotent on (auctionId, externalPaymentRef) by implementation.
     */
    Boolean paymentStatus(Long auctionId,
                                    Long payerUserId,
                                    BigDecimal amount,
                                    String externalPaymentRef);

    /**
     * Confirm that the settlement is fully paid.
     * Marks settlement as PAID and releases any remaining loser holds.
     */
    SettlementDto confirmPaid(Long auctionId);


    // ---------- Deadline / forfeiture / promotion ----------

    /**
     * Winner missed deadline: forfeit winner's hold and promote second-highest
     * as new winner with a new payment window. Returns promoted user id if any.
     */
    void expireAndPromoteNext(Long auctionId, LocalDateTime now);

    /**
     * Batch job to expire all overdue settlements and run promotion logic as needed.
     * Returns number of settlements processed.
     */
    int expireOverdueSettlements();


    // ---------- Cancellation ----------

    /**
     * Auction was canceled (seller/admin) after or during settlement.
     * Releases all holds and unwinds any settlement state.
     */
    void cancelAuctionSettlement(Long auctionId);


    // ---------- Queries ----------
    /**
     * Get the current settlement status for an auction.

    SettlementStatus getStatus(Long auctionId);
     */
}

