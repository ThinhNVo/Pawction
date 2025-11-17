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

    /**
     * Places a deposit hold for the given auction. Implementations must
     * compute {@code available} inside the same transaction and reject the
     * operation if insufficient funds are available.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @param amount    positive hold amount
     * @return the created hold
     * @throws IllegalArgumentException if amount is null/non-positive or insufficient
     */
    @Override
    public DepositHold placeHold(Long accountId, Long auctionId, BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount is required");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");

        Account account = getOrThrow(accountId);
        Auction auction = findAuctionById(auctionId);

        DepositHold hold = account.addHold(auction, amount);
        holdRepository.save(hold);
        accountRepository.save(account);

        return hold;
    }

    /**
     * Releases an active hold for the given auction back to the account.
     * No money moves; status changes from HELD to RELEASED.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @return the updated hold
     * @throws IllegalStateException if active hold is not found
     */
    @Override
    public DepositHold releaseHold(Long accountId, Long auctionId) {
        Account account = getOrThrow(accountId);
        DepositHold hold = account.getHolds().stream()
                .filter(h -> h.getDepositStatus() == Status.HELD && h.getAuction().getAuctionId().equals(auctionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active hold not found"));
        hold.setDepositStatus(Status.RELEASED);
        account.deposit(hold.getAmount());
        holdRepository.save(hold);
        accountRepository.save(account);
        return hold;
    }

    /**
     * Forfeits an active hold for the given auction (e.g., penalty or
     * auction rules). Implementations should post the corresponding debit
     * to the account ledger and flip the hold status to FORFEITED.
     *
     * @param accountId the account identifier
     * @param auctionId the auction identifier
     * @return the updated hold
     * @throws IllegalStateException if active hold is not found
     */
    @Override
    public DepositHold forfeitHold(Long accountId, Long auctionId) {
        Account account = getOrThrow(accountId);
        DepositHold hold = account.getHolds().stream()
                .filter(h -> h.getDepositStatus() == Status.HELD && h.getAuction().getAuctionId().equals(auctionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active hold not found"));
        hold.setDepositStatus(Status.FORFEITED);
        holdRepository.save(hold);
        return hold;
    }

    /**
     * Credits money to the account and records a transaction entry.
     *
     * @param accountId the account identifier
     * @param amount    positive amount to credit
     * @return the created transaction (credit)
     */
    @Override
    public Transaction deposit(Long accountId, BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount required");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");

        Account account = getOrThrow(accountId);
        Transaction tx = account.deposit(amount);
        txRepository.save(tx);
        accountRepository.save(account);
        return tx;
    }

    /**
     * Withdraw money from the account and records a transaction entry.
     *
     * @param accountId the account identifier
     * @param amount    positive amount to debit
     * @return the created transaction (debit)
     */
    @Override
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount required");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");

        Account account = getOrThrow(accountId);
        Transaction tx = account.withdraw(amount);
        txRepository.save(tx);
        accountRepository.save(account);
        return tx;
    }

    /**
     * Returns the current balance stored on the account database.
     *
     * @param accountId the account identifier
     * @return the ledger balance (non-null)
     */
    @Override
    public BigDecimal getBalance(Long accountId) {
        return getOrThrow(accountId).getBalance();
    }

    /**
     * Returns the current spendable amount after subtracting all HELD holds on account
     *
     * @param accountId the account identifier
     * @return available balance
     */
    @Override
    public BigDecimal getAvailable(Long accountId) {
        Account account = getOrThrow(accountId);
        return account.getBalance().subtract(account.getHolds().stream()
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .map(DepositHold::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Returns all deposit holds for the account, regardless of status.
     *
     * @param accountId the account identifier
     * @return list of holds (possibly empty)
     */
    @Override
    public List<DepositHold> getHolds(Long accountId) {
        return getOrThrow(accountId).getHolds();
    }

    /**
     * Returns only the active (HELD) deposit holds for the account.
     *
     * @param accountId the account identifier
     * @return list of active holds (possibly empty)
     */
    @Override
    public List<DepositHold> getActiveHolds(Long accountId) {
        return getOrThrow(accountId).getHolds().stream()
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .toList();
    }

    /**
     * Returns a paged slice of transactions for the account, ordered by
     * descending creation time
     *
     * @param accountId the account identifier
     * @return list of transactions (possibly empty)
     */
    @Override
    public List<Transaction> getTransactions(Long accountId) {
        return getOrThrow(accountId).getTransactions();
    }

    // ---------- helpers ----------

    /**
     * Fetches an account by id or throws if not found.
     *
     * @param accountId the account identifier
     * @return the account entity
     */
    private Account getOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + accountId));
    }

    /**
     * Fetches an auction by id or throws if not found.
     *
     * @param auctionId the auction identifier
     * @return the auction entity
     */
    private Auction findAuctionById(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalStateException("Auction not found: " + auctionId));
    }
}
