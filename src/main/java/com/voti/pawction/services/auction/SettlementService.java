package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.SettlementDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.AuctionMapper;
import com.voti.pawction.mappers.SettlementMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.auction.impl.SettlementServiceInterface;
import com.voti.pawction.services.wallet.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class SettlementService implements SettlementServiceInterface {
    private final SettlementMapper settlementMapper;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BiddingService biddingService;
    private final AccountService accountService;
    private final Clock clock;

    private static final int BATCH = 50;
    private final AuctionMapper auctionMapper;

    /**
     * Start settlement for a ended auction with a winner.
     *
     * @param auctionId
     * @param winnerUserId
     * @param paymentDueAt
     */
    @Override
    public SettlementDto begin(Long auctionId, Long winnerUserId, LocalDateTime paymentDueAt) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        var winningUser = getBidderOrThrow(winnerUserId);

        if (auction.getStatus() != Auction_Status.ENDED) {
            throw new AuctionInvalidStateException("Only ENDED auctions can begin settlement");
        }

        if (auction.getPaymentDueDate() == null) {
            auction.setPaymentDueDate(paymentDueAt);
        }

        var auctionList = auction.getDepositHolds();

        var firstBid =  biddingService.getWinningBid(auctionId);
        var secondBid = biddingService.getSecondHighestBid(auctionId);

        if (firstBid.isEmpty() && secondBid.isEmpty()) {
            return noWinner(auctionId);
        }

        for (var depositHold : auctionList) {
            if (!Objects.equals(depositHold.getAccount().getAccountId(),firstBid.get().getBidderId()) &&
            !Objects.equals(depositHold.getAccount().getAccountId(),secondBid.get().getBidderId())) {
                accountService.releaseHold(depositHold.getAccount().getAccountId(),auctionId);
            }
        }

        auction.setPaymentDueDate(paymentDueAt);
        auction.setWinningUser(winningUser);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        return settlementMapper.toDto(auctionRepository.save(auction));
    }


    /**
     * Handle the case where an auction ends with no winner or only 1 winner but didn't pay
     *
     * @param auctionId
     */
    @Override
    public SettlementDto noWinner(Long auctionId) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        auction.setWinningUser(null);
        auction.setPaymentDueDate(null);
        auction.setStatus(Auction_Status.SETTLED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        return settlementMapper.toDto(auctionRepository.save(auction));
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
    public Boolean paymentStatus(Long auctionId, Long payerUserId, BigDecimal amount, String externalPaymentRef) {
        return true;
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
    public void expireAndPromoteNext(Long auctionId, LocalDateTime now) {
        var auction = getAuctionOrThrow(auctionId);

        // TODO create payment method

        var firstWinningBid = biddingService.getWinningBid(auctionId);

        if (firstWinningBid.isEmpty()) {
            noWinner(auctionId);
            return;
        }

        var firstWinningBidder = getBidderOrThrow(firstWinningBid.get().getBidderId());

        accountService.forfeitHold(firstWinningBidder.getUserId(), auctionId);

        var secondWinningBid = biddingService.getSecondHighestBid(auctionId);


        if (secondWinningBid.isEmpty()) {
            noWinner(auctionId);
            return;
        }

        var secondWinningBidder = getBidderOrThrow(secondWinningBid.get().getBidderId());

        //auction.setHighestBid(secondWinningBidder.get);



    }

    /**
     * Batch job to expire all overdue settlements and run promotion logic as needed.
     * Returns number of settlements processed.
     */
    @Override
    public int expireOverdueSettlements() {
        int processed = 0;
        LocalDateTime nowNY = LocalDateTime.now(clock);

        List<Long> ids;
        do {
            ids = auctionRepository.findByStatusAndPaymentDueDateBefore(
                    Auction_Status.ENDED,
                    nowNY,
                    PageRequest.of(0, BATCH));

            for (Long id : ids) {
                expireAndPromoteNext(id, nowNY);
                processed++;
            }
            auctionRepository.flush();
        } while (!ids.isEmpty());

        return processed;
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
     * Fetches an auction by id or throws if not found.
     *
     * @param auctionId the account identifier
     * @return the account entity
     * @throws AuctionNotFoundException if the account doesn't exist
     */
    private Auction getAuctionOrThrowForUpdate(Long auctionId) {
        return auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(()-> new AuctionNotFoundException("Auction not found by id to update: " + auctionId));
    }

    /**
     * Fetches a user by id or throws if not found.
     *
     * @param userId the account identifier
     * @return the account entity
     * @throws UserNotFoundException if User is not found by id
     */
    private User getBidderOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found by id: " + userId));
    }

}
