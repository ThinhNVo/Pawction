package com.voti.pawction.services.auction;

import com.voti.pawction.dtos.request.AuctionRequest.CreateAuctionRequest;
import com.voti.pawction.dtos.request.AuctionRequest.UpdateAuctionDetailRequest;
import com.voti.pawction.dtos.response.AuctionDto;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.exceptions.AccountExceptions.AccountNotFoundException;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import com.voti.pawction.exceptions.AuctionExceptions.AuctionIdNotFoundException;
import com.voti.pawction.exceptions.PetNotFoundException;
import com.voti.pawction.exceptions.UserExceptions.UserNotFoundException;
import com.voti.pawction.mappers.AuctionMapper;
import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.repositories.auction.AuctionRepository;
import com.voti.pawction.repositories.pet.PetRepository;
import com.voti.pawction.services.auction.impl.AuctionServiceInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuctionServiceStub implements AuctionServiceInterface {
    private final AuctionMapper auctionMapper;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    /**
     * Create and immediately start a LIVE auction.
     * Highest bid is initialized to startPrice; status = LIVE.
     *
     * @param sellingUserId
     * @param petId
     * @param request
     */
    @Override
    public AuctionDto create(Long sellingUserId, Long petId, CreateAuctionRequest request) {
        requirePositive(request.getStartPrice());

        var sellingUser= getUserOrThrow(sellingUserId);
        var pet = getPetOrThrow(petId);



        Auction a = new Auction();
        a.setStartPrice(request.getStartPrice());
        a.setHighestBid(request.getStartPrice());
        a.setDescription(request.getDescription());
        a.setStatus(Auction_Status.LIVE);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setEndTime(request.getEndedAt());
        a.setSellingUser(sellingUser);
        a.setPet(pet);


        return auctionMapper.toDto(auctionRepository.save(a));
    }

    /**
     * Update mutable fields (pet information, description and endTime).
     * Guard illegal transitions (e.g., changing startPrice after LIVE).
     *
     * @param auctionId
     * @param petId
     * @param request
     */
    @Override
    public AuctionDto update(Long auctionId, Long petId, UpdateAuctionDetailRequest request) {
        return null;
    }

    /**
     * Seller ends the auction early (if policy allows). Transitions LIVE -> SETTLE.
     * Triggers winner selection & settlement coordination.
     *
     * @param auctionId Auction ID
     */
    @Override
    public AuctionDto settle(Long auctionId) {
        return null;
    }

    /**
     * Seller cancels the auction. Allowed only before first valid bid (policy).
     * Transitions LIVE -> CANCELED. Notifies watchers/bidders.
     *
     * @param auctionId
     * @param reason
     */
    @Override
    public void cancel(Long auctionId, String reason) {

    }

    /**
     * Called after ENDED to coordinate with SettlementService:
     * decide winner (if any), create payment deadline/hold instructions, etc.
     * Safe to call multiple times; must be idempotent.
     *
     * @param auctionId
     */
    @Override
    public AuctionDto end(Long auctionId) {
        return null;
    }

    /**
     * Close all auctions whose endTime <= now and are still LIVE.
     * Sets status=ENDED, picks provisional winner, sets payment deadline,
     * and coordinates with SettlementService. Returns number processed.
     */
    @Override
    public int closeEndedAuctions() {
        return 0;
    }


// ---------- helpers ----------
    /**
     * Validate the amount is larger or equal to 0 and not negative
     *
     * @param amt the amount
     * @throws InvalidAmountException    if insufficient available funds
     */
    private void requirePositive(BigDecimal amt) {
        Objects.requireNonNull(amt, "amount");
        if (amt.signum() <= 0) throw new InvalidAmountException("amount must be larger than 0");
    }

    /**
     * Amount a bidder must have available (balance or hold) to place a bid of 'bidAmount'.
     * Typically equals bidAmount + required deposit/fee, minus existing refundable hold.
     *
     * @param auctionId
     * @param bidAmount
     * @param bidderId
     */
    @Override
    public BigDecimal requiredAmountForBid(Long auctionId, BigDecimal bidAmount, Long bidderId) {
        return null;
    }

    /**
     * Validate min-increment rule given the current highest bid.
     *
     * @param auctionId
     * @param proposedBid
     */
    @Override
    public boolean isValidIncrement(Long auctionId, BigDecimal proposedBid) {
        return false;
    }

    /**
     * Compute the next valid minimum bid given current highest bid & increment policy.
     *
     * @param auctionId
     */
    @Override
    public BigDecimal nextMinimumBid(Long auctionId) {
        return null;
    }

    /**
     * Fetches a pet by id or throws if not found.
     *
     * @param petId the account identifier
     * @return the account entity
     * @throws AccountNotFoundException if account is not found by id
     */
    private Pet getPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(()-> new PetNotFoundException("Pet not found by id: " + petId));
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
     * @throws AuctionIdNotFoundException if the account doesn't exist
     */
    private Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(()-> new AuctionIdNotFoundException("Auction not found by id: " + auctionId));
    }
}
