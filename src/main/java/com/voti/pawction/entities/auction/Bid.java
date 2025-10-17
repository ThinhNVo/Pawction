package com.voti.pawction.entities.auction;

import com.voti.pawction.entities.auction.enums.Bid_Status;
import com.voti.pawction.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    public Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Bid_Status bidStatus;

    @Column(name="bid_Time", nullable = false)
    private LocalDateTime bidTime;

    //Bid to Auction Relation
    @ManyToOne
    @JoinColumn(name = "auction_id")
    @ToString.Exclude
    private Auction auction;

    //Bid to User Relation
    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;



}
