package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.response.BidDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.AccountExceptions.AccountNotFoundException;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.BidNotFoundException;
import com.voti.pawction.exceptions.BidExceptions.InvalidBidException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.BidMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.auction.BidRepository;
import com.voti.pawction.repositories.wallet.AccountRepository;
import com.voti.pawction.services.auction.impl.BiddingServiceInterface;
import com.voti.pawction.services.auction.policy.AuctionPolicy;
import com.voti.pawction.services.user.UserService;
import com.voti.pawction.services.wallet.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BiddingService implements BiddingServiceInterface {
    private final BidMapper bidMapper;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionPolicy auctionPolicy;
    private final UserRepository userRepository;
    private final AccountService accountService;

    private final Clock clock;

    /**
     * Places a bid on a LIVE auction on behalf of the given bidder.
     * <p>
     * Flow:
     * <ul>
     *   <li>Loads the bidder and auction with a row lock.</li>
     *   <li>Validates auction status and end time.</li>
     *   <li>Computes the required deposit hold amount via {@link AuctionPolicy#requireAmount(Long)}.</li>
     *   <li>Checks that the bid amount is a valid increment over the current highest bid.</li>
     *   <li>Ensures the bidder has sufficient available funds and places a wallet hold.</li>
     *   <li>Persists the bid as {@link Bid_Status#WINNING}.</li>
     *   <li>Marks all other bids on this auction as {@link Bid_Status#OUTBID} in bulk.</li>
     *   <li>Updates the auction's {@code highestBid} and {@code winningUser}.</li>
     * </ul>
     *
     * @param bidderId  the id of the bidding user
     * @param auctionId the id of the auction to bid on
     * @param amount    proposed bid amount (must be positive)
     * @return the created bid as a DTO
     * @throws AuctionInvalidStateException if the auction is not LIVE or already ended
     * @throws InvalidBidException          if the bid amount is not a valid increment
     * @throws InvalidAmountException       if the bidder has insufficient funds for the required hold
     * @throws UserNotFoundException        if the bidder id is not found
     * @throws AuctionNotFoundException     if the auction id is not found
     */
    @Override
    @Transactional
    public BidDto placeBid(Long bidderId, Long auctionId, BigDecimal amount) {
        var bidder = getUserOrThrow(bidderId);

        var auction = getAuctionOrThrowForUpdate(auctionId);

        requirePositive(amount);

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can accept bids");
        }

        if (!auction.getEndTime().isAfter(LocalDateTime.now(clock))) {
            throw new AuctionInvalidStateException("Auction already ended");
        }

        BigDecimal requiredHold = auctionPolicy.requireAmount(auctionId);

        if (!auctionPolicy.isValidIncrement(auctionId, amount)) {
            throw new InvalidBidException("Bid must be at least $1 higher than current highest bid");
        }

        if (accountService.getAvailable(bidderId).compareTo(requiredHold) < 0) {
            throw new InvalidBidException("Insufficient funds to cover required deposit hold");
        }

        accountService.placeHold(bidderId, auctionId, requiredHold);

        var bid = new Bid();
        bid.setAmount(amount);
        bid.setBidStatus(Bid_Status.WINNING);
        bid.setBidTime(LocalDateTime.now(clock));
        bid.setAuction(auction);
        bid.setUser(bidder);
        var saved = bidRepository.save(bid);

        bidRepository.bulkMarkOutbid(
                auctionId,
                saved.getBidId(),
                Bid_Status.OUTBID
        );

        auction.setHighestBid(amount);
        auction.setWinningUser(bidder);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        return bidMapper.toDto(bid);
    }

    /**
     * Returns the current winning bid for an auction, if any.
     * <p>
     * "Winning" is defined as the highest amount with earliest bid time
     * (tie-breaker), as implemented by the repository query.
     *
     * @param auctionId the auction identifier
     * @return an {@link Optional} with the winning bid DTO, or empty if no bids exist
     * @throws AuctionNotFoundException if the auction id is not found
     */
    @Override
    public Optional<BidDto> getWinningBid(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);
        return bidRepository.findTopByAuctionId(auction.getAuctionId())
                .map(bidMapper::toDto);
    }

    /**
     * Returns the second-highest bid for an auction, if any.
     * <p>
     * This is typically used for fallback logic when the provisional winner
     * fails to complete payment and the platform offers the item to the
     * runner-up.
     *
     * @param auctionId the auction identifier
     * @return an {@link Optional} with the second-highest bid DTO, or empty if fewer than two bids exist
     * @throws AuctionNotFoundException if the auction id is not found
     */
    @Override
    public Optional<BidDto> getSecondHighestBid(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);
        return bidRepository.findSecondByAuctionId(auction.getAuctionId())
                .map(bidMapper::toDto);
    }

    /**
     * Finalizes bid statuses when an auction is closed.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If no bids exist, does nothing.</li>
     *   <li>Marks the top bid as {@link Bid_Status#WON} if it was still WINNING.</li>
     *   <li>Marks all other bids on the auction as {@link Bid_Status#OUTBID}
     *       using a bulk update.</li>
     * </ul>
     * <p>
     * This method is idempotent and safe to call multiple times.
     *
     * @param auctionId the auction identifier
     * @throws AuctionNotFoundException if the auction id is not found
     * @throws BidNotFoundException     if the top bid cannot be reloaded unexpectedly
     */
    @Override
    public void finalizeBidsOnClose(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);

        var bids = bidRepository.findByAuctionIdOrderByAmountDescBidTimeAsc(auction.getAuctionId());

        if (bids.isEmpty()) {
            return;
        }

        var winningBid = bidRepository.findTopByAuctionId(auction.getAuctionId())
                .orElseThrow(()-> new BidNotFoundException("Bid not found by this id" + auctionId));

        if (winningBid.getBidStatus() == Bid_Status.WINNING) {
            winningBid.setBidStatus(Bid_Status.WON);
            bidRepository.save(winningBid);
        }

        bidRepository.bulkMarkOutbid(
                auctionId,
                winningBid.getBidId(),
                Bid_Status.OUTBID
        );
    }


/*
    @Override
    public List<BidDto> listTopBids(Long auctionId, int limit) {

        return List.of();
    }
 */

    /**
     * Ensures a monetary amount is strictly positive.
     * Use this before setting start prices, bids, deposits, or fees.
     *
     * @param amt the amount to validate (must be non-null and &gt; 0)
     * @throws NullPointerException   if {@code amt} is null
     * @throws InvalidAmountException if {@code amt} is zero or negative
     */
    private void requirePositive(BigDecimal amt) {
        Objects.requireNonNull(amt, "amount");
        if (amt.signum() <= 0) throw new InvalidAmountException("amount must be larger than 0");
    }

// ---------- Lookup ----------
    /**
     * Fetches a bid by id or throws if not found.
     *
     * @param bidId the bid identifier
     * @return the bid entity
     * @throws BidNotFoundException if the bid doesn't exist
     */
    @Transactional
    public Bid getBidOrThrow(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(()-> new BidNotFoundException("Bid not found by id: " + bidId));
    }

    /**
     * Fetches a user by id or throws if not found.
     *
     * @param userId the account identifier
     * @return the account entity
     * @throws UserNotFoundException if User is not found by id
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found by id: " + userId));
    }

    /**
     * Fetches an auction by id or throws if not found.
     *
     * @param auctionId the account identifier
     * @return the account entity
     * @throws AuctionNotFoundException if the account doesn't exist
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
}
