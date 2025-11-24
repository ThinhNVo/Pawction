package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionEndTimeRequest;
import com.voti.pawction.dtos.request.PetRequest.UpdatePetWhenAuctionLiveRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionInvalidStateException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionNotFoundException;
import com.voti.pawction.exceptions.AuctionExceptions.InvalidAuctionException;
import com.voti.pawction.exceptions.PetNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.AuctionMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.services.auction.impl.AuctionServiceInterface;
import com.voti.pawction.services.auction.policy.AuctionPolicy;
import com.voti.pawction.services.pet.PetService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Application service for Auction lifecycle: create, update, cancel, and end/close.
 *
 * Responsibilities:
 * - Create LIVE auctions (highestBid initialized to startPrice).
 * - Update mutable fields while LIVE (description, endTime, limited pet info).
 * - Cancel before first valid bid (policy).
 * - End/close auctions (seller-initiated or scheduled) and delegate post-close
 *   orchestration to SettlementService (winner + payment window) and RankingService.
 *
 * Concurrency:
 * - Write paths use PESSIMISTIC_WRITE where appropriate to avoid race conditions.
 * - End/close is idempotent to allow safe retries.
 *
 * Time:
 * - All times use the application zone (e.g., America/New_York) via injected Clock.
 */
@Service
@AllArgsConstructor
public class AuctionService implements AuctionServiceInterface {
    private final AuctionMapper auctionMapper;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuctionPolicy auctionPolicy;
    private final PetService petService;
    private final BiddingService biddingService;

    private static final int BATCH = 200;
    private final Clock clock;

    /**
     * Create and immediately start a LIVE auction.
     * Initializes highestBid to startPrice and sets endTime from the request.
     *
     * @param sellingUserId seller's user id
     * @param petId pet id to list
     * @param request payload with startPrice, description, endTime
     * @return created auction as DTO
     * @throws InvalidAmountException if startPrice <= 0
     * @throws InvalidAuctionException if description missing/blank or endTime invalid
     * @throws UserNotFoundException if seller not found
     * @throws PetNotFoundException if pet not found
     */
    @Override
    @Transactional
    public AuctionDto create(Long sellingUserId, Long petId, CreateAuctionRequest request) {
        requirePositive(request.getStartPrice());

        requireFuture(LocalDateTime.now(), request.getEndedAt());

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new InvalidAuctionException("Auction description is required");
        }

        var sellingUser = getUserOrThrow(sellingUserId);

        var pet = getPetOrThrow(petId);

        Auction a = new Auction();
        a.setStartPrice(request.getStartPrice());
        a.setHighestBid(request.getStartPrice());
        a.setDescription(request.getDescription());
        a.setStatus(Auction_Status.LIVE);
        a.setCreatedAt(LocalDateTime.now(clock));
        a.setUpdatedAt(LocalDateTime.now(clock));
        a.setEndTime(request.getEndedAt());
        a.setSellingUser(sellingUser);
        a.setPet(pet);

        return auctionMapper.toDto(auctionRepository.save(a));
    }

    /**
     * Update mutable auction details (currently description) while LIVE.
     *
     * @param auctionId auction identifier
     * @param request new description
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if description missing/blank
     */
    @Override
    @Transactional
    public AuctionDto updateAuctionDetail(Long auctionId, UpdateAuctionDetailRequest request) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can be updated");
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new InvalidAuctionException("Auction description is required");
        }

        auction.setDescription(request.getDescription());
        auction.setUpdatedAt(LocalDateTime.now(clock));

        return auctionMapper.toDto(auctionRepository.save(auction));
    }

    /**
     * Update the auction end time while LIVE.
     * Business rule: new end time must be in the future and after creation;
     * optionally within a configured window.
     *
     * @param auctionId auction identifier
     * @param request wrapper for new end time
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if new end time invalid
     */
    @Override
    @Transactional
    public AuctionDto updateAuctionEndTime(Long auctionId, UpdateAuctionEndTimeRequest request) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        requireFuture(auction.getCreatedAt(), request.getNewEndTime());

        requireEndAbout12HoursFromNow(request.getNewEndTime());

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can be updated");
        }

        auction.setEndTime(request.getNewEndTime());
        auction.setUpdatedAt(LocalDateTime.now(clock));

        return auctionMapper.toDto(auctionRepository.save(auction));
    }

    /**
     * Update limited pet info during a LIVE auction (seller-only).
     *
     * @param auctionId auction identifier
     * @param sellerId authenticated seller id (must match auction.sellingUser.id)
     * @param request allowable pet fields to update during a live listing
     * @return updated auction DTO
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws IllegalArgumentException if caller is not the seller
     */
    @Override
    @Transactional
    public AuctionDto updateAuctionPetInfo(Long auctionId, Long sellerId, UpdatePetWhenAuctionLiveRequest request) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can be updated");
        }

        if (!Objects.equals(auction.getSellingUser().getUserId(), sellerId)) {
            throw new IllegalArgumentException("Only the seller can update pet details during a live auction");
        }

        petService.updatePetWhenAuctionLive(auction.getPet().getPetId(), auctionId, request);

        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        return auctionMapper.toDto(auction);
    }

    /**
     * Seller ends the auction early.
     * Transitions LIVE -> ENDED immediately and then delegates to {@link #end(Long)} for post-close orchestration.
     * Idempotent: if already ENDED, simply delegates to {@code end()}.
     *
     * @param auctionId Auction ID
     * @return ended auction DTO (post-close state applied)
     * @throws AuctionInvalidStateException if auction is CANCELED or already SETTLED
     */
    @Override
    @Transactional
    public AuctionDto settle(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can be settled");
        }

        auction.setStatus(Auction_Status.SETTLED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auction.setEndTime(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        return end(auction.getAuctionId());
    }

    /**
     * Post-close orchestration for an auction (idempotent).
     *
     * <p>This method ensures the auction is in the LIVE or SETTLE state, then performs the
     * following post-close logic:
     * <ul>
     *   <li>If the auction is still LIVE, it is transitioned to ENDED.</li>
     *   <li>If the auction is still SETTLED, it is transitioned to ENDED.</li>
     *   <li>Determines if there is a winning bid using {@code BiddingService}.</li>
     *   <li>If no bids are present → delegates to {@code settlementService.noWinner(auctionId)}
     *       to release holds and finalize the auction.</li>
     *   <li>If there is a winner:
     *       <ul>
     *         <li>Stamps {@code provisionalWinnerUserId} and {@code finalPrice}.</li>
     *         <li>If no {@code paymentDueAt} is set, assigns a payment deadline
     *             (e.g., now + 72h, or via configuration).</li>
     *         <li>Delegates to {@code settlementService.begin(...)} to initiate the
     *             settlement workflow.</li>
     *       </ul>
     *   </li>
     *   <li>Notifies {@code rankingService.onAuctionClosed(...)} for leaderboard/UI updates.</li>
     * </ul>
     *
     * <p>This method is safe to call multiple times — it checks the auction state and only applies
     * missing post-close operations, making it suitable for both interactive calls and scheduler retries.</p>
     *
     * @param auctionId the ID of the auction to finalize
     * @return the resulting {@link AuctionDto}, reflecting ENDED state and any winner/payment metadata
     * @throws AuctionInvalidStateException if the auction is CANCELED or already ENDED
     */
    @Override
    @Transactional
    public AuctionDto end(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() == Auction_Status.CANCELED || auction.getStatus() == Auction_Status.ENDED) {
            throw new AuctionInvalidStateException("Only LIVE or SETTLED auctions can be ended");
        }

        auction.setStatus(Auction_Status.ENDED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        // TODO handle if auction have no winner
        if (biddingService.getWinningBid(auctionId).isEmpty()) {
            //settlementService.noWinner(auctionId);
            return auctionMapper.toDto(auction);
        }

        biddingService.finalizeBidsOnClose(auctionId);

        auction.setPaymentDueDate(LocalDateTime.now(clock).plusHours(72));
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);

        // TODO start SettlementService
        //settlementService.begin(a.getAuctionId(), win.getBidderId(), win.getAmount(), a.getPaymentDueAt());

        return auctionMapper.toDto(auction);

    }

    /**
     * Cancel a LIVE auction before the first valid bid.
     * Transitions LIVE -> CANCELED and releases all holds via SettlementService.
     *
     * @param auctionId auction identifier
     * @throws AuctionInvalidStateException if auction is not LIVE
     * @throws InvalidAuctionException if a first bid already occurred
     */
    @Override
    @Transactional
    public void cancel(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);

        if (auction.getStatus() != Auction_Status.LIVE) {
            throw new AuctionInvalidStateException("Only LIVE auctions can be canceled");
        }

        if (!Objects.equals(auction.getHighestBid(), auction.getStartPrice())) {
            throw new InvalidAuctionException("Cannot cancel after the first bid");
        }

        auction.setStatus(Auction_Status.CANCELED);
        auction.setUpdatedAt(LocalDateTime.now(clock));
        auctionRepository.save(auction);
    }

    /**
     * Close all auctions whose endTime <= now and are still LIVE.
     * For each candidate, flips to ENDED (with locking) and delegates to {@link #end(Long)}.
     *
     * @return number of auctions processed
     */
    @Override
    @Transactional
    public int closeExpiredAuctions() {
        int processed = 0;
        LocalDateTime nowNY = LocalDateTime.now(clock);

        List<Long> ids;
        do {
            ids = auctionRepository.findIdsByStatusAndEndTimeLte(
                    Auction_Status.LIVE, nowNY, PageRequest.of(0, BATCH));

            for (Long id : ids) {
                processed += closeOneIfExpired(id, nowNY) ? 1 : 0;
            }
            auctionRepository.flush();
        } while (!ids.isEmpty());

        return processed;
    }

    /**
     * Close a single auction if it has expired, then delegate post-close orchestration to {@link #end(Long)}.
     * This method acquires a PESSIMISTIC_WRITE lock on the auction row to prevent
     * concurrent closes/bids, flips LIVE -&gt; ENDED when {@code endTime &le nowNY},
     * stamps {@code updatedAt} (and {@code paymentDueAt} if missing), persists the change,
     * and finally calls {@link #end(Long)} to perform winner selection and settlement coordination.
     *
     * @param auctionId    the auction id to evaluate
     * @param nowNY the current timestamp in application time zone (e.g., America/New_York)
     * @return {@code true} if the auction was transitioned from LIVE to ENDED and {@code end(id)} was invoked;
     *         {@code false} if the auction was not LIVE or not yet expired
     */
    @Transactional
    @Override
    public boolean closeOneIfExpired(Long auctionId, LocalDateTime nowNY) {
        var auction = getAuctionOrThrowForUpdate(auctionId);

        if (auction.getStatus() != Auction_Status.LIVE || auction.getEndTime().isAfter(nowNY)) return false;

        end(auction.getAuctionId());
        return true;
    }

// ---------- helpers ----------


    /**
     * Validate that the auction end time is in the future and after creation.
     * enforce a reasonable duration window (12 hours).
     * Intended to be called during create/update flows while the auction is LIVE.
     *
     * @param createdAt the timestamp the auction was (or will be) created
     * @param endedAt   the requested end time for the auction
     * @throws InvalidAuctionException if either timestamp is null, if {@code endedAt}
     *                                 is not strictly after {@code createdAt}, or if
     *                                 the duration falls outside the allowed window
     */
    private void requireFuture(LocalDateTime createdAt, LocalDateTime endedAt) {
        if (createdAt != null && endedAt != null) {
            if (endedAt.isBefore(createdAt)) {
                throw new InvalidAuctionException("Auction end time must be in the future");
            }

            if (!Duration.between(createdAt, endedAt)
                    .equals(Duration.ofHours(12))) {
                throw new InvalidAuctionException("Auction start time and end time must be 12 hours apart");
            }
            return;
        }
        throw new InvalidAuctionException("Auction create time and end time are required");
    }

    /**
     * Validate that a new auction end time is approximately 12 hours from "now".
     *
     * This helper is intended for update flows, where the business rule is that
     * the adjusted end time should be scheduled around 12 hours from the current
     * application time. It compares the duration between {@code now} (via the
     * injected {@link Clock}) and the requested {@code endedAt} value and enforces
     * the configured 12-hour window.
     *
     * @param endedAt the proposed new end time for the auction
     * @throws InvalidAuctionException if {@code endedAt} is null, in the past, or
     *                                 if the duration between {@code now} and
     *                                 {@code endedAt} does not satisfy the 12-hour rule
     */
    private void requireEndAbout12HoursFromNow(LocalDateTime endedAt) {
        if (!Duration.between(LocalDateTime.now(clock), endedAt)
                .equals(Duration.ofHours(12))) {
            throw new InvalidAuctionException("Auction new end time must be 12 hours from today");
        }
    }

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

    /**
     * Compute the next minimum bid (no min-increment policy).
     * Rule: highestBid + 1 unit (adjust if you support sub-unit currency).
     *
     * @param auctionId auction identifier
     * @return next minimum allowed bid
     */
    @Transactional
    public BigDecimal nextMinimumBid(Long auctionId) {
        var auction = getAuctionOrThrow(auctionId);
        return auction.getHighestBid().add(BigDecimal.ONE);
    }

    /**
     * Fetches an auction by id or throws if not found.
     *
     * @param auctionId the account identifier
     * @return the account entity
     * @throws AuctionNotFoundException if the account doesn't exist
     */
    @Transactional
    public Auction getAuctionOrThrowForUpdate(Long auctionId) {
        return auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(()-> new AuctionNotFoundException("Auction not found by id to update: " + auctionId));
    }

    /**
     * Fetches an auction by id from auction policy or throws if not found.
     *
     * @param auctionId the account identifier
     * @return the account entity
     * @throws AuctionNotFoundException if the account doesn't exist
     */
    @Transactional
    public Auction getAuctionOrThrow(Long auctionId) {
        return auctionPolicy.getAuctionOrThrow(auctionId);
    }
    
    /**
     * Fetches a pet by id or throws if not found.
     *
     * @param petId the account identifier
     * @return the account entity
     * @throws PetNotFoundException if pet is not found by id
     */
    private Pet getPetOrThrow(Long petId) {
        return  petService.getPetOrThrow(petId);
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
}
