package com.voti.pawction.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name="auctions")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auctionId;
    @Column(name = "pet_id", nullable = false)
    private Long petId;
    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;
    @Column(name = "winner_user_id")
    private Long winnerUserId;
    @Column(name = "start_price", nullable = false)
    private Double startPrice;
    @Column(name = "highest_bid", nullable = false)
    private Double highestBid;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


