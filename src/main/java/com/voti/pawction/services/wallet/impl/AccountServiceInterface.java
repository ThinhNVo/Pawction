package com.voti.pawction.services.wallet.impl;

import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.wallet.Transaction;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AccountServiceInterface {

    // ---- Lookup ----
    /**
     * Fetches an account by id or throws if not found.
     *
     * @param accountId the account identifier
     * @return the account entity
     * @throws java.util.NoSuchElementException if the account doesn't exist
     */
    Account get(Long accountId);

    /**
     * Returns the current balance stored on the account database.
     * This is the raw balance before subtracting any active holds.
     *
     * @param accountId the account identifier
     * @return the ledger balance (non-null)
     */
    BigDecimal getBalance(Long accountId);

    /**
     * Returns the current spendable amount after subtracting all HELD holds on account
     * query in the database as:
     * balance - SUM(HELD holds). Implementation should execute this inside
     * a transaction and may use a locking query when called from mutation
     * flows (e.g., withdraw/placeHold) to prevent double-spend.
     */
    BigDecimal getAvailable(Long accountId); // derived at read time

    // ---- Holds ----
    /**
     * Returns all deposit holds for the account, regardless of status.
     *
     * @param accountId the account identifier
     * @return list of holds (possibly empty)
     */
    List<DepositHold> getHolds(Long accountId);

    /**
     * Returns only the active (HELD) deposit holds for the account.
     *
     * @param accountId the account identifier
     * @return list of active holds (possibly empty)
     */
    List<DepositHold> getActiveHolds(Long accountId);

    /**
     * Places a deposit hold for the given auction. Implementations must
     * compute {@code available} inside the same transaction and reject the
     * operation if insufficient funds are available.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @param amount    positive hold amount
     * @return the created hold
     * @throws IllegalArgumentException if amount is null/non-positive
     * @throws IllegalStateException    if insufficient available funds
     */
    DepositHold placeHold(Long accountId, Long auctionId, BigDecimal amount);

    /**
     * Releases an active hold for the given auction back to the account.
     * No money moves; status changes from HELD to RELEASED.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @return the updated hold
     * @throws IllegalStateException if an active hold is not found
     */
    DepositHold releaseHold(Long accountId, Long auctionId);

    /**
     * Forfeits an active hold for the given auction (e.g., penalty or
     * auction rules). Implementations should post the corresponding debit
     * to the account ledger and flip the hold status to FORFEITED.
     *
     * @param accountId the account identifier (explicit to avoid ambiguity)
     * @param auctionId the auction identifier
     * @return the updated hold
     * @throws IllegalStateException if an active hold is not found
     */
    DepositHold forfeitHold(Long accountId, Long auctionId);

    // ---- Money movements ----
    /**
     * Credits money to the account and records a transaction entry.
     *
     * @param accountId the account identifier
     * @param amount    positive amount to credit
     * @return the created transaction (credit)
     * @throws IllegalArgumentException if amount is null/non-positive
     */
    Transaction deposit(Long accountId, BigDecimal amount);

    /**
     * Withdraw money from the account and records a transaction entry.
     * Implementations must compute {@code available} inside the same
     * transaction and reject if insufficient.
     *
     * @param accountId the account identifier
     * @param amount    positive amount to debit
     * @return the created transaction (debit)
     * @throws IllegalArgumentException if amount is null/non-positive
     * @throws IllegalStateException    if insufficient available funds
     */
    Transaction withdraw(Long accountId, BigDecimal amount);


    // ---- History ----
    /**
     * Returns a paged slice of transactions for the account, ordered by
     * descending creation time unless otherwise documented by the impl.
     *
     * @param accountId the account identifier
     * @return list of transactions (possibly empty)
     * @throws IllegalArgumentException if page/size are invalid
     */
    List<Transaction> getTransactions(Long accountId);



}
