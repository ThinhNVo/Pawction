package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Payment_Status;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PaymentExceptions.InvalidPaymentException;
import com.voti.pawction.exceptions.PaymentExceptions.UnauthorizedPaymentException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.AuctionMapper;
import com.voti.pawction.mappers.BidMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.auction.impl.SettlementServiceInterface;
import com.voti.pawction.services.wallet.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Application service responsible for post-auction settlement.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Kick off the settlement window for a closed auction and its winner.</li>
 *   <li>Handle the “no winner” scenario (no bids or failure to pay).</li>
 *   <li>Validate and record a payment for the winning bidder.</li>
 *   <li>Expire overdue settlements and promote the second-highest bidder when applicable.</li>
 *   <li>Cancel settlement (e.g. admin/seller cancel) and release all deposit holds.</li>
 * </ul>
 *
 * <p>Concurrency:</p>
 * <ul>
 *   <li>Write paths use {@code getAuctionOrThrowForUpdate(..)}, which relies on a
 *       PESSIMISTIC_WRITE query in {@link AuctionRepository}, to avoid
 *       race conditions when multiple workers touch the same auction.</li>
 *   <li>Most operations are written to be safe to call repeatedly; if
 *       preconditions are not met, the methods short-circuit and become no-ops.</li>
 * </ul>
 *
 * <p>Time:</p>
 * <ul>
 *   <li>All time comparisons rely on the injected {@link Clock}, allowing tests
 *       to fix the “now” value and making the logic deterministic.</li>
 * </ul>
 */
@Service
@AllArgsConstructor
public class SettlementService implements SettlementServiceInterface {
    private final AuctionMapper auctionMapper;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BiddingService biddingService;
    private final AccountService accountService;
    private final Clock clock;

    private static final int BATCH = 50;
    private final BidMapper bidMapper;

    /**
     * Begins the settlement phase for an auction that has already ENDED and has a winner.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Loads the auction with a row lock.</li>
     *   <li>Validates that the auction status is {@link Auction_Status#ENDED}.</li>
     *   <li>Loads the first-place (winner) user and the optional second-highest bid.</li>
     *   <li>If the auction has no bids, delegates to {@link #noWinner(Long)}.</li>
     *   <li>Iterates over deposit holds to release non-winner (and, depending on policy,
     *       non-runner-up) holds back to their accounts via {@link AccountService#releaseHold(Long, Long)}.</li>
     *   <li>Stamps the winner and {@code paymentDueAt} deadline on the auction.</li>
     *   <li>Persists the updated auction and returns a {@link AuctionDto} view.</li>
     * </ul>
     *
     * @param auctionId    the identifier of the auction to start settlement for
     * @param winnerUserId the user id of the winning bidder
     * @param paymentDueAt the deadline until which the winner may complete payment
     * @return a {@link AuctionDto} representing the auction state
     *
     * @throws AuctionNotFoundException   if the auction id does not exist
     * @throws UserNotFoundException      if the winner user id does not exist
     * @throws AuctionInvalidStateException if the auction is not in {@code ENDED} status
     */
    @Override
    public AuctionDto begin(Long auctionId, Long winnerUserId, LocalDateTime paymentDueAt) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getStatus() != Auction_Status.ENDED) {
            throw new AuctionInvalidStateException("Only ENDED auctions can begin settlement");
        }

        var firstPlace = getBidderOrThrow(winnerUserId);
        var secondPlace = biddingService.getSecondHighestBid(auctionId);

        if (auction.getBids().isEmpty()) {
            return noWinner(auctionId);
        }

        var firstPlaceUserId = firstPlace.getUserId();
        var secondPlaceUserId = secondPlace.map(BidDto::getBidderId).orElse(null);


        for (var depositHold : auction.getDepositHolds()) {
            System.out.println(depositHold);
            if (secondPlaceUserId != null) {
                if (!depositHold.getAccount().getAccountId().equals(firstPlaceUserId)
                        && !depositHold.getAccount().getAccountId().equals(secondPlaceUserId)) {
                    accountService.releaseHold(depositHold.getAccount().getAccountId(),auctionId);
                }
            } else {
                if (!depositHold.getAccount().getAccountId().equals(firstPlaceUserId)) {
                    accountService.releaseHold(depositHold.getAccount().getAccountId(),auctionId);
                }
            }
        }

        auction.setPaymentDueDate(paymentDueAt);
        auction.setWinningUser(firstPlace);
        auction.setUpdatedAt(LocalDateTime.now(clock));

        return auctionMapper.toDto(auctionRepository.save(auction));
    }


    /**
     * Marks an ENDED auction as having no winner and finalizes it as SETTLED.
     *
     * <p>This is used when:</p>
     * <ul>
     *   <li>the auction ended with no valid bids, or</li>
     *   <li>any previous winner has effectively failed to pay and the platform
     *       decides to close the auction without awarding it to anyone.</li>
     * </ul>
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Loads the auction with a row lock.</li>
     *   <li>Ensures the auction is in {@link Auction_Status#ENDED}.</li>
     *   <li>Clears the {@code winningUser} and {@code paymentDueDate} fields.</li>
     *   <li>Sets the status to {@link Auction_Status#SETTLED} and updates timestamps.</li>
     *   <li>Persists the auction and returns a {@link AuctionDto} snapshot.</li>
     * </ul>
     *
     * @param auctionId the identifier of the auction to finalize without a winner
     * @return a {@link AuctionDto} representing the settled, no-winner state
     *
     * @throws AuctionNotFoundException   if the auction id does not exist
     * @throws AuctionInvalidStateException if the auction is not in {@code ENDED} status
     */
    @Override
    public AuctionDto noWinner(Long auctionId) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getStatus() != Auction_Status.ENDED) {
            throw new AuctionInvalidStateException("Auction must be ENDED to mark no winner");
        }

        auction.setWinningUser(null);
        auction.setPaymentDueDate(null);
        auction.setStatus(Auction_Status.SETTLED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        return auctionMapper.toDto(auctionRepository.save(auction));
    }

    /**
     * Validates and records a payment for the winning bidder of an ENDED auction.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>The payment amount must be non-null and strictly greater than zero.</li>
     *   <li>The currency must be provided and be one of the supported currency codes (currently USD only).</li>
     *   <li>The auction must be in {@link Auction_Status#ENDED} status.</li>
     *   <li>The auction must have a non-null winning user.</li>
     *   <li>The {@code payerUserId} must match the winning user id.</li>
     *   <li>The payment window ({@code paymentDueAt}) must be configured and not expired.</li>
     *   <li>If a bidding amount exists, the payment amount must exactly equal the final bid price.</li>
     * </ul>
     *
     * <p>On success:</p>
     * <ul>
     *   <li>Marks {@link Payment_Status#PAID} on the auction.</li>
     *   <li>Clears the payment due date.</li>
     *   <li>Transitions the auction to {@link Auction_Status#SETTLED}.</li>
     *   <li>Updates the {@code updatedAt} timestamp and persists the auction.</li>
     * </ul>
     *
     * <p>Note: This method currently records payment at the auction level only; the actual
     * money movement or wallet ledger entries are expected to be handled elsewhere.</p>
     *
     * @param auctionId   the auction being paid for
     * @param payerUserId the user id of the paying (winning) bidder
     * @param amount      the payment amount, expected to match the winning bid
     * @param currency    ISO-like currency code (e.g. {@code "USD"})
     *
     * @throws AuctionNotFoundException       if the auction id does not exist
     * @throws UserNotFoundException          if the payer user id does not exist
     * @throws AuctionInvalidStateException   if the auction state or payment window is invalid
     * @throws UnauthorizedPaymentException   if a non-winner attempts to pay
     * @throws InvalidPaymentException        if amount or currency are invalid
     */
    @Override
    public void paymentRecord(Long auctionId, Long payerUserId, BigDecimal amount, String currency) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        var seller = auction.getSellingUser();

        var winner = getBidderOrThrow(payerUserId);

        var winningUser = auction.getWinningUser();

        var paymentDueDate = auction.getPaymentDueDate();

        var now = LocalDateTime.now(clock);

        var biddingAmount = auction.getHighestBid();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        if (currency == null || currency.isBlank()) {
            throw new InvalidPaymentException("Currency must be provided");
        }

        Set<String> supportedCurrencies = Set.of("USD");
        if (!supportedCurrencies.contains(currency.trim().toUpperCase())) {
            throw new InvalidPaymentException("Unsupported currency: " + currency);
        }

        if (auction.getStatus() != Auction_Status.ENDED) {
            throw new AuctionInvalidStateException("Only ENDED auctions can accept payment");
        }

        if (winningUser == null) {
            throw new AuctionInvalidStateException("Auction has no winner and cannot accept payment");
        }

        if (!Objects.equals(winningUser.getUserId(), payerUserId)) {
            throw new UnauthorizedPaymentException("Only the winning user can pay for this auction");
        }

        if (paymentDueDate == null) {
            throw new AuctionInvalidStateException("Payment window is not configured for this auction");
        }

        if (paymentDueDate.isBefore(now)) {
            throw new AuctionInvalidStateException("Payment window has expired for this auction");
        }

        if (biddingAmount != null && amount.compareTo(biddingAmount) != 0) {
            throw new InvalidPaymentException(
                    "Payment amount must match the bidding bid amount: " + biddingAmount
            );
        }

        auction.setPaymentStatus(Payment_Status.PAID);
        auction.setWinningUser(winner);
        auction.setPaymentDueDate(null);
        auction.setStatus(Auction_Status.SETTLED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        accountService.deposit(seller.getUserId(), biddingAmount);
    }


    /**
     * Handles an overdue settlement by forfeiting the current winner's deposit hold
     * and optionally promoting the second-highest bidder as the new winner.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Loads the auction with a row lock.</li>
     *   <li>Short-circuits if the auction is already paid.</li>
     *   <li>Short-circuits if the auction is not {@code ENDED} or has no winner.</li>
     *   <li>Checks {@code paymentDueAt} against {@code now}; returns early if still within the window.</li>
     *   <li>If no second-highest bid exists:
     *     <ul>
     *       <li>Forfeits the current winner's hold via {@link AccountService#forfeitHold(Long, Long)}.</li>
     *       <li>Resets {@code highestBid} to {@code startPrice}.</li>
     *       <li>Delegates to {@link #noWinner(Long)} to settle the auction without a winner.</li>
     *     </ul>
     *   </li>
     *   <li>If a second-highest bid exists:
     *     <ul>
     *       <li>Forfeits the current winner’s hold.</li>
     *       <li>Promotes the runner-up as the new winner.</li>
     *       <li>Updates {@code highestBid} to the runner-up’s amount.</li>
     *       <li>Sets a new payment window (e.g. now + 72 hours).</li>
     *       <li>Persists the updated auction.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>If preconditions are not met (e.g. auction already paid, not ENDED, or payment window still open),
     * this method does nothing and returns immediately.</p>
     *
     * @param auctionId the auction whose settlement is overdue
     * @param now       the current timestamp (typically {@code LocalDateTime.now(clock)})
     *
     * @throws AuctionNotFoundException   if the auction id does not exist
     * @throws UserNotFoundException      if the promoted user cannot be loaded
     */
    @Override
    public boolean expireAndPromoteNext(Long auctionId, LocalDateTime now) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getPaymentStatus() == Payment_Status.PAID) {
            return false;
        }

        if (auction.getStatus() != Auction_Status.ENDED || auction.getWinningUser() == null) {
            return false;
        }

        var due = auction.getPaymentDueDate();
        if (due == null || due.isAfter(now)) {
            return false;
        }

        var currentWinner = auction.getWinningUser();
        var secondPlace = biddingService.getSecondHighestBid(auctionId);

        if (secondPlace.isEmpty()) {
            accountService.forfeitHold(currentWinner.getUserId(), auctionId);
            auction.setHighestBid(auction.getStartPrice());
            auctionRepository.save(auction);
            noWinner(auctionId);
            return true;
        }

        var backupWinner = getBidderOrThrow(secondPlace.get().getBidderId());

        if (!currentWinner.equals(backupWinner)) {
            accountService.forfeitHold(currentWinner.getUserId(), auctionId);

            auction.setWinningUser(backupWinner);
            auction.setHighestBid(secondPlace.get().getAmount());
            auction.setPaymentDueDate(LocalDateTime.now(clock).plusHours(72));
            auction.setUpdatedAt(LocalDateTime.now(clock));
            auctionRepository.save(auction);
            return true;
        }

        accountService.forfeitHold(currentWinner.getUserId(), auctionId);
        auction.setHighestBid(auction.getStartPrice());
        auctionRepository.save(auction);
        noWinner(auctionId);
        return true;
    }

    /**
     * Batch job that iterates over all ENDED auctions whose payment window has expired
     * and applies {@link #expireAndPromoteNext(Long, LocalDateTime)} to each.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Determines {@code now} from the injected {@link Clock}.</li>
     *   <li>Fetches candidate auction ids in pages of {@link #BATCH} size using
     *       {@link AuctionRepository findByStatusAndPaymentDueDateBefore(Auction_Status, LocalDateTime, PageRequest)}.</li>
     *   <li>Calls {@link #expireAndPromoteNext(Long, LocalDateTime)} for each candidate id.</li>
     *   <li>Flushes the repository between batches to apply updates incrementally.</li>
     * </ul>
     *
     * @return the number of auctions processed (i.e. candidates passed to {@code expireAndPromoteNext})
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
                if (expireAndPromoteNext(id, nowNY)) {
                    processed++;
                }
            }
            auctionRepository.flush();
        } while (!ids.isEmpty());

        return processed;
    }

    /**
     * Cancels settlement for an auction that has already entered or completed settlement.
     *
     * <p>Typical use cases:</p>
     * <ul>
     *   <li>Admin or seller cancels the auction after the settlement window has started.</li>
     *   <li>Operational rollback of a problematic settlement.</li>
     * </ul>
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Loads the auction with a row lock.</li>
     *   <li>Releases <strong>all</strong> deposit holds for that auction back to their accounts
     *       via {@link AccountService#releaseHold(Long, Long)}.</li>
     *   <li>Clears the winning user and payment due date.</li>
     *   <li>Resets {@code highestBid} to the {@code startPrice}.</li>
     *   <li>Sets status to {@link Auction_Status#CANCELED} and updates timestamps.</li>
     *   <li>Persists the updated auction.</li>
     * </ul>
     *
     * @param auctionId the identifier of the auction whose settlement should be canceled
     *
     * @throws AuctionNotFoundException if the auction id does not exist
     */
    @Override
    public void cancelAuctionSettlement(Long auctionId) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        for (var depositHold : auction.getDepositHolds()) {
            accountService.releaseHold(depositHold.getAccount().getAccountId(),auctionId);
        }

        auction.setWinningUser(null);
        auction.setPaymentDueDate(null);
        auction.setHighestBid(auction.getStartPrice());
        auction.setStatus(Auction_Status.CANCELED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);
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
