package com.voti.pawction.services.wallet;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.wallet.Transaction;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.repositories.wallet.DepositHoldRepository;
import com.voti.pawction.repositories.wallet.TransactionRepository;
import com.voti.pawction.services.wallet.impl.AccountServiceInterface;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
@Transactional
public class AccountServiceStub implements AccountServiceInterface {
    private final AccountRepository accountRepository;
    private final DepositHoldRepository holdRepository;
    private final AuctionRepository auctionRepository;
    private final TransactionRepository txRepository;

    // ---------- helpers ----------
    private static BigDecimal requirePositive(BigDecimal amt) {
        Objects.requireNonNull(amt, "amount");
        if (amt.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");
        return amt;
    }

    /**
     * Fetches an account by id or throws if not found.
     *
     * @param accountId the account identifier
     * @return the account entity
     * @throws java.util.NoSuchElementException if the account doesn't exist
     */
    @Override
    public Account get(Long accountId) {
        return accountRepository.findById(accountId).orElseThrow();
    }

    /**
     * Returns the current balance stored on the account database.
     * This is the raw balance before subtracting any active holds.
     *
     * @param accountId the account identifier
     * @return the ledger balance (non-null)
     */
    @Override
    public BigDecimal getBalance(Long accountId) {
        return get(accountId).getBalance();
    }

    /**
     * Returns the current spendable amount after subtracting all HELD holds on account
     * query in the database as:
     * balance - SUM(HELD holds). Implementation should execute this inside
     * a transaction and may use a locking query when called from mutation
     * flows (e.g., withdraw/placeHold) to prevent double-spend.
     */
    @Override
    public BigDecimal getAvailable(Long accountId) {
        BigDecimal available = accountRepository.computeAvailable(accountId);
        if (available != null) return available;
        return BigDecimal.ZERO;
    }

    /**
     * Returns all deposit holds for the account, regardless of status.
     *
     * @param accountId the account identifier
     * @return list of holds (possibly empty)
     */
    @Override
    public List<DepositHold> getHolds(Long accountId) {
        return get(accountId).getHolds();
    }

    /**
     * Returns only the active (HELD) deposit holds for the account.
     *
     * @param accountId the account identifier
     * @return list of active holds (possibly empty)
     */
    @Override
    public List<DepositHold> getActiveHolds(Long accountId) {
        return getHolds(accountId).stream()
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .toList();
    }

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
    @Override
    public DepositHold placeHold(Long accountId, Long auctionId, BigDecimal amount) {
        requirePositive(amount);
        if (getAvailable(accountId).compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient available funds for hold");
        }
        var auction = auctionRepository.findById(auctionId).orElseThrow();

        var a = get(accountId);
        var hold = holdRepository.save(a.addHold(auction, amount));
        accountRepository.save(a);

        return hold;
    }

    /**
     * Releases an active hold for the given auction back to the account.
     * No money moves; status changes from HELD to RELEASED.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @return the updated hold
     * @throws IllegalStateException if an active hold is not found
     */
    @Override
    public DepositHold releaseHold(Long accountId, Long auctionId) {
        var auctionHold = auctionRepository.findById(auctionId).orElseThrow().getDepositHolds();

        var releaseHold = auctionHold.stream()
                .filter(h -> Objects.equals(h.getAccount().getAccountId(), accountId))
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active hold not found for account on this auction"));
        releaseHold.setDepositStatus(Status.RELEASED);

        var a = get(accountId);
        a.deposit(releaseHold.getAmount());
        return holdRepository.save(releaseHold);
    }

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
    @Override
    public DepositHold forfeitHold(Long accountId, Long auctionId) {
        var auctionHold = auctionRepository.findById(auctionId).orElseThrow().getDepositHolds();

        var penaltyHold = auctionHold.stream()
                .filter(h -> Objects.equals(h.getAccount().getAccountId(), accountId))
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active hold not found for account on this auction"));

        penaltyHold.setDepositStatus(Status.FORFEITED);
        return holdRepository.save(penaltyHold);
    }

    /**
     * Credits money to the account and records a transaction entry.
     *
     * @param accountId the account identifier
     * @param amount    positive amount to credit
     * @return the created transaction (credit)
     * @throws IllegalArgumentException if amount is null/non-positive
     */
    @Override
    public Transaction deposit(Long accountId, BigDecimal amount) {
        requirePositive(amount);
        Account a = get(accountId);
        Transaction deposit = txRepository.save(a.deposit(amount));
        accountRepository.save(a);
        return deposit;
    }

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
    @Override
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        requirePositive(amount);
        if (getAvailable(accountId).compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient available funds");
        }
        Account a = get(accountId);
        Transaction withdraw = a.withdraw(amount);
        accountRepository.save(a);
        return withdraw;
    }

    /**
     * Returns a paged slice of transactions for the account, ordered by
     * descending creation time unless otherwise documented by the impl.
     *
     * @param accountId the account identifier
     * @return list of transactions (possibly empty)
     * @throws IllegalArgumentException if page/size are invalid
     */
    @Override
    public List<Transaction> getTransactions(Long accountId) {
        return get(accountId).getTransactions();
    }
}
