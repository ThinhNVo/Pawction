package com.voti.pawction.repositories.auction;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.enums.Bid_Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query(value = """
        SELECT * FROM Bid b
        WHERE b.auction_id = :auctionId\s
        ORDER BY amount DESC, bid_time \s
        LIMIT 1\s
       \s""", nativeQuery = true)
    Optional<Bid> findTopByAuctionId(@Param("auctionId") Long auctionId);

    @Query(value = """
        SELECT * FROM Bid b
        WHERE b.auction_id = :auctionId\s
        ORDER BY amount DESC, bid_time \s
        LIMIT 1 OFFSET 1;\s
       \s""", nativeQuery = true)
    Optional<Bid> findSecondByAuctionId(@Param("auctionId") Long auctionId);


    @Query(value = """
        SELECT * FROM Bid b
        WHERE b.auction_id = :auctionId\s
        ORDER BY amount DESC, bid_time \s
       \s""", nativeQuery = true)
    Optional<Bid> findByAuctionIdOrderByAmountDescBidTimeAsc(@Param("auctionId")Long auctionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Bid b
            SET b.bidStatus = :status
            WHERE b.auction.auctionId = :auctionId
              AND b.bidId <> :winningBidId
            """)
    int bulkMarkOutbid(@Param("auctionId") Long auctionId,
                       @Param("winningBidId") Long winningBidId,
                       @Param("status") Bid_Status status);

    Optional<Bid> findTopByAuction_AuctionIdAndUser_UserIdOrderByAmountDesc(Long auctionId, Long userId);

    int countByAuction_AuctionId(Long auctionId);

    List<Bid> findByAuction_AuctionIdOrderByBidTimeDesc(Long auctionId);

    Optional<Bid> findTopByAuction_AuctionIdOrderByAmountDesc(Long auctionId);

    boolean existsByUser_UserIdAndAuction_AuctionId(Long userId, Long auctionId);

    void deleteBidByAmount(BigDecimal amount);
}
