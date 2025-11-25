package com.voti.pawction.repositories.auction;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.auction.enums.Auction_Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("""
        select a.auctionId from Auction a
        where a.status = :status
          and a.endTime <= :cutoff
        order by a.endTime asc
    """)
    List<Long> findIdsByStatusAndEndTimeLte(
            @Param("status") Auction_Status status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable page
    );

    @Query("""
       select a.auctionId
       from Auction a
       where a.status = :status
         and a.paymentDueDate <= :cutoff
       """)
    List<Long> findByStatusAndPaymentDueDateBefore(
            @Param("status") Auction_Status status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable page
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.auctionId = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);


}
