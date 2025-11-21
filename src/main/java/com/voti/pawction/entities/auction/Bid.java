package com.voti.pawction.entities.auction;

import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
    private Long bidId;

    @Column(nullable = false)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    public Bid_Status bidStatus;

    @Column(name="bid_Time", nullable = false)
    private LocalDateTime bidTime;

    //Bid to Auction Relation
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auction_id")
    @ToString.Exclude
    private Auction auction;

    //Bid to User Relation
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;
}
