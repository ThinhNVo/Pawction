package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.SettlementDto;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.mappers.SettlementMapper;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.auction.impl.SettlementServiceInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class SettlementService implements SettlementServiceInterface {
    private final SettlementMapper settlementMapper;
    private final AuctionRepository auctionRepository;

    /**
     * Start settlement for a closed auction with a winner.
     * Creates/updates a settlement record in PENDING_PAYMENT state.
     *
     * @param auctionId
     * @param winnerUserId
     * @param finalPrice
     * @param paymentDueAt
     */
    @Override
    public SettlementDto begin(Long auctionId, Long winnerUserId, BigDecimal finalPrice, LocalDateTime paymentDueAt) {
        return null;
    }

    /**
     * Handle the case where an auction ends with no winner or only 1 winner but didn't pay
     *
     * @param auctionId
     */
    @Override
    public void noWinner(Long auctionId) {

    }

    /**
     * Winner chooses how to handle their existing hold:
     * - APPLY_TO_PAYMENT: apply HELD amount up to final price; returns remaining due.
     * - RELEASE_TO_ACCOUNT: release hold; returns full price due.
     *
     * @param auctionId
     * @param winnerUserId
     * @param choice
     */
    @Override
    public BigDecimal chooseHoldOption(Long auctionId, Long winnerUserId, HoldChoice choice) {
        return null;
    }

    /**
     * Record a payment attempt for this auction.
     * Idempotent on (auctionId, externalPaymentRef) by implementation.
     *
     * @param auctionId
     * @param payerUserId
     * @param amount
     * @param externalPaymentRef
     */
    @Override
    public PaymentReceiptDto recordPayment(Long auctionId, Long payerUserId, BigDecimal amount, String externalPaymentRef) {
        return null;
    }

    /**
     * Confirm that the settlement is fully paid.
     * Marks settlement as PAID and releases any remaining loser holds.
     *
     * @param auctionId
     */
    @Override
    public SettlementDto confirmPaid(Long auctionId) {
        return null;
    }

    /**
     * Winner missed deadline: forfeit winner's hold and promote second-highest
     * as new winner with a new payment window. Returns promoted user id if any.
     *
     * @param auctionId
     * @param now
     */
    @Override
    public Optional<Long> expireAndPromoteNext(Long auctionId, LocalDateTime now) {
        return null;
    }

    /**
     * Batch job to expire all overdue settlements and run promotion logic as needed.
     * Returns number of settlements processed.
     */
    @Override
    public int expireOverdueSettlements() {
        return 0;
    }

    /**
     * Auction was canceled (seller/admin) after or during settlement.
     * Releases all holds and unwinds any settlement state.
     *
     * @param auctionId
     */
    @Override
    public void cancelAuctionSettlement(Long auctionId) {

    }

    /**
     * Get settlement info for an auction, if it exists.
     *
     * @param auctionId
     */
    private Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(()-> new AuctionNotFoundException("Auction not found by id: " + auctionId));
    }

    /**
     * Get the current settlement status for an auction.
     *
     * @param auctionId
     */
    @Override
    public SettlementStatus getStatus(Long auctionId) {
        return null;
    }
}
