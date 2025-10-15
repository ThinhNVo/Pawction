package com.voti.pawction.entities.auction;

import com.voti.pawction.entities.auction.enums.Bid_Status;
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
@Table(name = "bid")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bidId;

    @Column(nullable = false)
    public int auctionId;

    @Column(nullable = false)
    public int userId;

    @Column(nullable = false)
    public Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Bid_Status bidStatus;

    @Column(name="bid_Time", nullable = false)
    private LocalDateTime bidTime;






}
