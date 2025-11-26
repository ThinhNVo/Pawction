package com.voti.pawction.services.auction.impl;


import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.dtos.response.SettlementDto;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Payment_Status;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.PaymentExceptions.InvalidPaymentException;
import com.voti.pawction.exceptions.PaymentExceptions.UnauthorizedPaymentException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.wallet.AccountService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

public interface SettlementServiceInterface {
    // ---------- Kickoff when auction closes with a winner ----------

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
    AuctionDto begin(Long auctionId,
                        Long winnerUserId,
                        LocalDateTime paymentDueAt);

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
    AuctionDto noWinner(Long auctionId);

    // ---------- Payment lifecycle ----------

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
    void paymentRecord(Long auctionId,
                                    Long payerUserId,
                                    BigDecimal amount,
                                    String currency);



    // ---------- Deadline / forfeiture / promotion ----------

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
    boolean expireAndPromoteNext(Long auctionId, LocalDateTime now);

    /**
     * Batch job that iterates over all ENDED auctions whose payment window has expired
     * and applies {@link #expireAndPromoteNext(Long, LocalDateTime)} to each.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Determines {@code now} from the injected {@link Clock}.</li>
     *   <li>Fetches candidate auction ids in pages of Batch size using
     *       {@link AuctionRepository findByStatusAndPaymentDueDateBefore(Auction_Status, LocalDateTime, PageRequest)}.</li>
     *   <li>Calls {@link #expireAndPromoteNext(Long, LocalDateTime)} for each candidate id.</li>
     *   <li>Flushes the repository between batches to apply updates incrementally.</li>
     * </ul>
     *
     * @return the number of auctions processed (i.e. candidates passed to {@code expireAndPromoteNext})
     */
    int expireOverdueSettlements();


    // ---------- Cancellation ----------

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
    void cancelAuctionSettlement(Long auctionId);
}

